package paquete;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.regex.MatchResult;

/**
 * Created by miguelvp on 21/03/2017.
 * Clase encargada de la gestión de avisos al recibir la señal del temporizador.
 */

public class Peticiones extends BroadcastReceiver {

    private Utils utiles;
    private NotificationManager manNotif;
    private Notification.Builder builder;

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        manNotif = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        builder = new Notification.Builder(context);
        if(manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            utiles = Utils.getSingleton();
            utiles.openData(context);
            HashMap<String, Integer> peticiones = utiles.getPeticiones();
            HashSet<String> nombres = new HashSet<>(peticiones.keySet());
            String[] pasar = new String[nombres.size()];
            int i = 0;
            for(String nombre: nombres){
                pasar[i] = nombre;
                i++;
            }
            Pedir comunicaciones = new Pedir(context);
            comunicaciones.execute(pasar);
        }
    }

    /**
     * Recibe un string en una única tira y obtiene las posiciones en las que se mandó el instrumento
     * lo que indica que esos están libres.
     * @param in Tira de las posiciones que están libres.
     * @return Posiciones de los que están libres.
     */
    private int[] descodificarPeticiones(String in){
        int[] devolver;
        String posiciones;
        String[] tempPos;
        Scanner sc;
        try {
            sc = new Scanner(in);
            sc.findInLine("libres='(.*)'");
            MatchResult data = sc.match();
            posiciones = data.group(1);
            sc.close();
            tempPos = posiciones.split(", ");
            devolver = new int[tempPos.length];
            for(int i = 0; i < tempPos.length; i++){
                devolver[i] = Integer.parseInt(tempPos[i]);
            }
        }catch (Exception e) {
            return null;
        }
        return devolver;
    }

    /**
     * Clase encargada de gestionar la comunicación para avisar de que un instrumento ha quedado libre.
     */
    private class Pedir extends AsyncTask<String, Void, InputStream> {

        private String[] peticiones;
        private HttpURLConnection http;
        private Context contexto;

        Pedir(Context c){
            contexto = c;
        }

        /**
         * Escribe sobre el buffer pasado los nombres de los instrumentos en formato nombre#='...'&
         * @param writer Buffer sobre el que escribir los instrumentos.
         * @param nombres Nombres de los instrumentos a consultar.
         */
        private void escribeBuffer(BufferedWriter writer, String[] nombres){
            try{
                for(int i = 0; i < nombres.length; i++){
                    writer.write("nombre" + (i+1) + "=" + utiles.pasarHexadecimal(nombres[i]));
                    if(i != nombres.length - 1)
                        writer.write("&");
                }
            } catch (Exception e){
            }
        }

        /**
         * Inicia la comunicación con el servidor.
         * @param params Array de instrumentos sobre los que se ha indicado generar el aviso.
         * @return El stream recibido del servidor para su descodificación.
         */
        @Override
        protected InputStream doInBackground(String... params) {
            peticiones = params;
            URL url;
            URLConnection urlC;
            InputStream in = null;
            OutputStream out;
            try {
                url = new URL(utiles.getDirBase() + "/instrumentos/peticiones.php");
            } catch (Exception e){
                return null;
            }
            try {
                urlC = url.openConnection();
            } catch (IOException e){
                return null;
            }
            http = (HttpURLConnection) urlC;
            try {
                http.setReadTimeout(10000);
                http.setConnectTimeout(10000);
                http.setRequestMethod("POST");
                http.setDoOutput(true);
                http.setDoInput(true);
                out = http.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "utf-8"));
                escribeBuffer(writer, peticiones);
                writer.flush();
                out.close();
            } catch (Exception e){
            }
            try {
                if (http.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    in = null;
                } else {
                    if (!url.getHost().equals(http.getURL().getHost())) {
                        return null;
                    }
                    in = new BufferedInputStream(http.getInputStream());
                }
            }catch (Exception e){

            }
            return in;
        }

        /**
         * Descodifica la respuesta recibida y actúa en consecuencia.
         * @param stream Stream recibido por el servidor.
         */
        @Override
        protected void onPostExecute(InputStream stream) {

            if (stream != null) {
                String datosEntrada = utiles.leerDatos(stream); // libres='posiciones de los instrumentos pasados'
                if (datosEntrada.equals("Null\n")){
                } else {
                    int[] posicionesLibres = descodificarPeticiones(datosEntrada);
                    Uri sonido = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    for(int j: posicionesLibres){
                        try {
                            String nombre = peticiones[j - 1];
                            int pos = utiles.getIdPeticion(nombre);
                            builder.setContentTitle(nombre);
                            builder.setContentText(contexto.getString(R.string.notif_aviso));
                            builder.setSmallIcon(R.drawable.notiavisob);
                            builder.setSound(sonido);
                            manNotif.notify(Utils.TAG_PETICIONES, pos, builder.build());
                            utiles.borrarPeticion(nombre);
                        }catch (Exception e){
                        }
                    }
                }
            } else {
            }
            http.disconnect();
        }
    }
}
