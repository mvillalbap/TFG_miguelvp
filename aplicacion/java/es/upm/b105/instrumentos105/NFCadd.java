package paquete;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by miguelvp on 01/12/2016.
 * Actividad encargada de leer las etiquetas y de su procesado tanto para reservar como para borrar.
 */

public class NFCadd extends Activity{

    private NotificationManager manNotif;
    private Notification.Builder builder;
    private Utils utiles;
    private int a;
    private boolean borrar;

    private NdefMessage vacio;
    private Ndef nuevo;

    @Override
    protected void onCreate(Bundle savedInstanceData){
        super.onCreate(savedInstanceData);
        utiles = Utils.getSingleton();
        utiles.openData(this);
        borrar = utiles.getBorrado();
        if (!utiles.isLogged()){
            Toast.makeText(this,R.string.t_loggin,Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        manNotif = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null || !ni.isConnected()) {
            Toast.makeText(this, R.string.t_connect, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        Intent intent = getIntent();
        nuevo = Ndef.get((Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG));
        String nombre = leeNombre(nuevo);
        if (!borrar) {
            if (nombre == null) {
                finish();
                return;
            }

            a = utiles.contiene(nombre);
            String usuario = utiles.getNombre();
            switch (a) {

                case 0:
                    Uri sonido = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    builder = new Notification.Builder(this);
                    builder.setContentTitle(nombre);
                    builder.setContentText(getString(R.string.notif_reserva));
                    builder.setSmallIcon(R.drawable.logonotif);
                    builder.setSound(sonido);
                    builder.setAutoCancel(true);
                    builder.setOngoing(true);
                    break;

                default:
                    break;

            }

            Reservar reser = new Reservar();
            reser.execute(nombre, usuario);

        } else {
            try {
                if (nuevo != null) {
                    if (nombre == null) {
                        Toast.makeText(this, R.string.t_noName, Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    nuevo.connect();
                    NdefRecord grabar = null;
                    try {
                        grabar = new NdefRecord(NdefRecord.TNF_EMPTY, null,
                                null, null);
                    } catch (Exception e) {
                        finish();
                        return;
                    }
                    vacio = new NdefMessage(grabar);
                    nuevo.writeNdefMessage(vacio);
                    nuevo.close();
                    utiles.setBorrado(false);
                    Borrar erase = new Borrar();
                    erase.execute(nombre, utiles.getNombre());
                }
            } catch (Exception e){
                Toast.makeText(getApplicationContext(), R.string.t_eraseProblem, Toast.LENGTH_LONG).show();
                finish();
            }
        }
        finish();
    }

    /**
     * Realiza la lectura del tag pasado como parámetro.
     * @param tag Tag del que leer la información.
     * @return Nombre del instrumento que tiene almacenado el tag.
     */
    public String leeNombre(Ndef tag){
        NdefMessage mensaje;
        try {
            tag.connect();
            mensaje = tag.getNdefMessage();
            tag.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        try {
            NdefRecord[] fragmentos = mensaje.getRecords();
            for (NdefRecord mssg : fragmentos) {
                if (mssg.getTnf() == NdefRecord.TNF_WELL_KNOWN) {
                    byte[] a = NdefRecord.RTD_TEXT;
                    if (mssg.getType()[0] == a[0]) {
                        try {
                            return new String(mssg.getPayload(), "UTF-8");
                        } catch (Exception e) {
                            return null;
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this, R.string.t_fail, Toast.LENGTH_LONG).show();
        }
        return null;
    }

    /**
     * Clase encargada de gestionar la comunicación para el proceso de borrado de un tag.
     */
    private class Borrar extends AsyncTask<String, Void, InputStream> {

        private String nombre, user;
        private HttpURLConnection http;

        /**
         * Inicia la comunicación con el servidor.
         * @param params Recibe como parámetros:
         *               <ul><li>[0] contiene el nombre del instrumento.</li>
         *               <li>[1] contiene el nombre del usuario.</li></ul>
         * @return El stream recibido del servidor para su descodificación.
         */
        @Override
        protected InputStream doInBackground(String... params) {
            nombre = params[0];
            user = params[1];
            URL url;
            URLConnection urlC;
            InputStream in = null;
            OutputStream out;
            try {
                url = new URL(utiles.getDirBase() + "/instrumentos/borrar.php");
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
                writer.write("usuario=" + utiles.pasarHexadecimal(user) + "&nombre=" + utiles.pasarHexadecimal(nombre));
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
                String datosEntrada = utiles.leerDatos(stream);
                if (datosEntrada.equals("OK\n")){
                    Toast.makeText(getApplicationContext(), R.string.t_erase, Toast.LENGTH_SHORT).show();
                    /*try{
                        borraTag();
                    }catch (Exception e){
                        Toast.makeText(getApplicationContext(), R.string.t_eraseProblem, Toast.LENGTH_LONG).show();
                    }*/
                }else{
                    Toast.makeText(getApplicationContext(), R.string.t_noErase, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), R.string.t_server, Toast.LENGTH_SHORT).show();
            }
            http.disconnect();
        }
    }

    /**
     * Clase encargada de gestionar la comunicación para la reserva de un instrumento.
     */
    private class Reservar extends AsyncTask<String, Void, InputStream> {

        private String nombre, user;
        private HttpURLConnection http;

        /**
         * Inicia la comunicación con el servidor.
         * @param params Recibe como parámetros:
         *               <ul><li>[0] contiene el nombre del instrumento.</li>
         *               <li>[1] contiene el nombre del usuario.</li></ul>
         * @return El stream recibido del servidor para su descodificación.
         */
        @Override
        protected InputStream doInBackground(String... params) {
            nombre = params[0];
            user = params[1];
            URL url;
            URLConnection urlC;
            InputStream in = null;
            OutputStream out;
            try {
                url = new URL(utiles.getDirBase() + "/instrumentos/reservar.php");
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
                writer.write("usuario=" + utiles.pasarHexadecimal(user) +
                        "&nombre=" + utiles.pasarHexadecimal(nombre));
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
                String datosEntrada = utiles.leerDatos(stream);
                if (datosEntrada.equals("OK\n")){
                    int pos = utiles.guardar(nombre);
                    if(pos != Utils.ERROR)
                        manNotif.notify(Utils.TAG_RESERVAS, pos, builder.build());
                    Toast.makeText(getApplicationContext(), R.string.t_res, Toast.LENGTH_SHORT).show();
                    utiles.borrarRegistro(nombre, System.currentTimeMillis()/1000, getApplicationContext());
                }else if (datosEntrada.equals("False\n")){
                    Toast.makeText(getApplicationContext(), R.string.t_yetRes, Toast.LENGTH_SHORT).show();
                }else if (datosEntrada.equals("True\n")){
                    int res = utiles.borrar(nombre);
                    if(res != Utils.ERROR)
                        manNotif.cancel(Utils.TAG_RESERVAS, a);
                    Toast.makeText(getApplicationContext(), R.string.t_free, Toast.LENGTH_SHORT).show();
                    utiles.guardarRegistro(nombre, System.currentTimeMillis()/1000, getApplicationContext());
                }else if (datosEntrada.equals("Null\n")){
                    Toast.makeText(getApplicationContext(), R.string.t_noExist, Toast.LENGTH_LONG).show();
                }else if (datosEntrada.equals("Failed connection")) {
                    Toast.makeText(getApplicationContext(), R.string.t_user, Toast.LENGTH_LONG).show();
                }else{
                }
            } else {
                Toast.makeText(getApplicationContext(), R.string.t_server, Toast.LENGTH_SHORT).show();
            }
            http.disconnect();
        }
    }


}
