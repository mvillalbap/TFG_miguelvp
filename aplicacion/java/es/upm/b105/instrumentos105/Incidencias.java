package paquete;

import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.database.MatrixCursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Locale;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by miguelvp on 10/07/2017.
 */

public class Incidencias extends ListActivity {

    private boolean ventana, vacio;
    private AlertDialog cuadro;
    private Utils utiles;
    private TreeMap<Long, String> entradas;
    private String inst;
    private int cuenta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_incidencias);
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null || !ni.isConnected()) {
            Toast.makeText(getApplicationContext(), R.string.t_connect, Toast.LENGTH_SHORT).show();
            finish();
        }
        utiles = Utils.getSingleton();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.server_connection);
        builder.setView(new ProgressBar(this));
        builder.setMessage(R.string.t_rec_data);
        builder.setNegativeButton(R.string.cancelar, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ventana = false;
                finish();
            }
        });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                ventana = false;
            }
        });
        cuadro = builder.create();
        cuadro.show();
        inst = getIntent().getExtras().getString(Utils.USER_SESSION);
        vacio = true;
        More mas = new More(this);
        mas.execute(inst, "5");
        cuenta = 5;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id){
        if (utiles.isLogged() && !vacio) {
            ListAdapter la = l.getAdapter();
            final long fechaLong = ((MatrixCursor) la.getItem(position)).getLong(2);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_item);
            adapter.add(entradas.get(fechaLong));
            builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                }
            });
            cuadro = builder.create();
            cuadro.show();
        }
    }

    public void load(View v){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.server_connection);
        builder.setView(new ProgressBar(this));
        builder.setMessage(R.string.t_rec_data);
        builder.setNegativeButton(R.string.cancelar, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ventana = false;
                finish();
            }
        });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                ventana = false;
            }
        });
        cuadro = builder.create();
        cuadro.show();
        inst = getIntent().getExtras().getString(Utils.USER_SESSION);
        vacio = true;
        cuenta += 5;
        More mas = new More(this);
        mas.execute(inst, Integer.toString(cuenta));
    }

    /**
     * Realiza una descodificación de la tira de instrumentos pasada como parámetros donde además
     * se encuentran indicados el usuario que lo tiene reservado y la fecha desde cuándo. El
     * resultado es devuelto en la matriz recorrida por un cursor pasada como parámetro.
     * @param in Tira de nombres de los instrumentos junto al usuario de la reserva y la fecha.
     * @param cursor Matriz donde se devolverán los instrumentos y su información ya descodificada.
     */
    private TreeMap<Long, String> descodificarLista(String in, MatrixCursor cursor){
        String[] lineas = in.split("<siguienteIncidencia'>");
        TreeMap<Long, String> devolver = new TreeMap<>();
        MatrixCursor.RowBuilder builder;
        char[] incidenciaAux = new char[1];
        for (int j = 1; j < lineas.length; ++j){
            Pattern pattern = Pattern.compile("\\nincidencias='([%\\w]+)'[\\s]+fecha=([0-9]+)[\\n]+");
            Pattern pattern2 = Pattern.compile("\\nincidencias=''[\\s]+fecha=([0-9]+)[\\n]+");
            Matcher matcher = pattern.matcher(lineas[j]);
            Matcher matcher2 = pattern2.matcher(lineas[j]);
            String incidencia, formato, mesDia;
            long fechaLong = 0;
            if(matcher.find()) {
                try {
                    fechaLong = Long.parseLong(matcher.group(2));
                    incidencia = matcher.group(1);
                    incidenciaAux = new char[incidencia.length()/3];
                    for(int i = 0; i < incidencia.length(); i += 3){
                        int sal = Integer.parseInt(incidencia.substring(i+1,i+3),16);
                        incidenciaAux[i/3] = (char) sal;
                    }
                } catch (Exception e){}
            }else if(matcher2.find()) {
                try {
                    fechaLong = Long.parseLong(matcher2.group(1));
                    incidenciaAux = new char[1];
                    incidenciaAux[0] = ' ';
                } catch (Exception e){}
            }else{
                incidenciaAux = new char[1];
                incidenciaAux[0] = ' ';
                fechaLong = (long) 1;
            }
            incidencia = new String(incidenciaAux);
            try {
                devolver.put(fechaLong, incidencia);
                Date fecha = new Date(fechaLong * 1000);
                String minutos = (fecha.getMinutes() > 9)?(fecha.getMinutes()+""):("0" + fecha.getMinutes());
                if (Locale.getDefault().getDisplayLanguage().equals("español"))
                    mesDia = fecha.getDate() + "/" + (fecha.getMonth() + 1);
                else
                    mesDia = (fecha.getMonth() + 1) + "/" + fecha.getDate();
                formato = getString(R.string.incidencia) + " " + mesDia + " (" + fecha.getHours() + ":" + minutos + ")";
            }catch(Exception e){
                formato = getString(R.string.free);
            }
            builder = cursor.newRow();
            builder.add(j).add(formato).add(fechaLong);
        }
        return devolver;
    }

    /**
     * Clase encargada de gestionar la comunicación para obtener todos los instrumentos que existan
     * que cumplan ciertas condiciones.
     */
    private class More extends AsyncTask<String, Void, InputStream> {

        private HttpURLConnection http;
        private Context context;

        More(Context c){
            context = c;
        }

        /**
         * Inicia la comunicación con el servidor.
         * @param params Recibe como parámetros:
         *               <ul><li>[0] contiene el nombre a buscar.</li>
         *               <li>[1] contiene el tipo de búsqueda.</li></ul>
         * @return El stream recibido del servidor para su descodificación.
         */
        @Override
        protected InputStream doInBackground(String... params) {
            URL url;
            URLConnection urlC;
            InputStream in = null;
            try {
                url = new URL(utiles.getDirBase() + "/instrumentos/incidencias.php?_query=" + utiles.pasarHexadecimal(params[0]) + "&_cantidad=" + params[1]);
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
                http.setRequestMethod("GET");
                http.setDoOutput(false);
                http.setDoInput(true);
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

            vacio = false;

            if (stream != null && !ventana) {
                String datosEntrada = utiles.leerDatos(stream); //nombre=''&reserva=''&fecha='0'
                MatrixCursor cursor = new MatrixCursor(new String[] {"_id", "Fecha", "fechaLong"});
                if(datosEntrada.equals("Null\n")) {
                    vacio = true;
                    cursor.newRow().add(1).add(getString(R.string.sinIncidencias)).add(""); //add(context.getString(R.string.res_empty)).add("");
                }else {
                    vacio = false;
                    entradas = descodificarLista(datosEntrada, cursor);
                }
                SimpleCursorAdapter adapter = new SimpleCursorAdapter(context, android.R.layout.simple_list_item_1, cursor, new String[] {"Fecha"}, new int[] {android.R.id.text1});
                setListAdapter(adapter);
            } else {
                Toast.makeText(getApplicationContext(), R.string.t_server, Toast.LENGTH_LONG).show();
            }
            ventana = false;
            http.disconnect();
            cuadro.dismiss();
        }
    }
}
