package paquete;

import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.MatchResult;

/**
 * Created by miguelvp on 24/11/2016.
 * Clase encargada de las consultas sobre el estado de los instrumentos.
 */

public class ReservasActivity extends ListActivity {

    private boolean ventana;
    private AlertDialog cuadro;
    private Utils utiles;
    private RadioButton nom;
    private EditText buscar;
    private boolean vacio;

    @Override
    protected void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);
        setContentView(R.layout.layout_reservas);
        ventana = false;
        buscar = (EditText) findViewById(R.id.Busqueda);
        nom = (RadioButton) findViewById(R.id.nomb_option);
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

        Todos todos = new Todos(this);
        String busqueda = buscar.getText().toString();
        String tipo = (nom.isChecked())?"usuario":"instrumento";
        todos.execute(busqueda, tipo);
        buscar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean puls = false;
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    empezar();
                    puls = true;
                }
                return puls;
            }
        });
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id){
        if (utiles.isLogged() && !vacio) {
            ListAdapter la = l.getAdapter();
            final String inst = ((MatrixCursor) la.getItem(position)).getString(1);
            boolean libre = (((MatrixCursor) la.getItem(position)).getString(2)).equals(getString(R.string.free));
            boolean aviso = utiles.isPedido(inst);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_item);
            if(!libre) {
                adapter.add(getString(R.string.simpleIncidencia));
                adapter.add((aviso) ? getString(R.string.borrar) : getString(R.string.crear));
                builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String mssg = adapter.getItem(which);
                        if (mssg != null) {
                            if (mssg.equals(getString(R.string.borrar))) {
                                utiles.borrarPeticion(inst);
                            } else if (mssg.equals(getString(R.string.crear))) {
                                utiles.setPeticion(inst);
                            } else if (mssg.equals(getString(R.string.simpleIncidencia))){
                                Intent verIncidencias = new Intent(getApplicationContext(), Incidencias.class);
                                verIncidencias.putExtra(Utils.USER_SESSION, inst);
                                startActivity(verIncidencias);
                            }
                        }
                    }
                });
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                    }
                });
                cuadro = builder.create();
                cuadro.show();
            }else{
                adapter.add(getString(R.string.simpleIncidencia));
                builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String mssg = adapter.getItem(which);
                        if (mssg != null) {
                            if (mssg.equals(getString(R.string.simpleIncidencia))){
                                Intent verIncidencias = new Intent(getApplicationContext(), Incidencias.class);
                                verIncidencias.putExtra(Utils.USER_SESSION, inst);
                                startActivity(verIncidencias);
                            }
                        }
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

    }

    /**
     * Muestra por pantalla una ventana emergente para que el usuario espere y comienza la
     * comunicación con el servidor.
     * @param v Botón de búsqueda.
     */
    public void buscar(View v){
        empezar();
    }

    /**
     * Realiza una descodificación de la tira de instrumentos pasada como parámetros donde además
     * se encuentran indicados el usuario que lo tiene reservado y la fecha desde cuándo. El
     * resultado es devuelto en la matriz recorrida por un cursor pasada como parámetro.
     * @param in Tira de nombres de los instrumentos junto al usuario de la reserva y la fecha.
     * @param cursor Matriz donde se devolverán los instrumentos y su información ya descodificada.
     */
    private void descodificarLista(String in, MatrixCursor cursor){
        String[] lineas = in.split("\n");
        Scanner sc;
        MatrixCursor.RowBuilder builder;
        for (int j = 0; j < lineas.length; ++j){
            sc = new Scanner(lineas[j]);
            MatchResult data;
            String formato, mesDia;
            try {
                sc.findInLine("nombre='(.*)'&reserva='(\\w+)'&fecha='(\\d+)'");
                data = sc.match();
                Date fecha = new Date(Long.parseLong(data.group(3)) * 1000);
                String minutos = (fecha.getMinutes() > 9)?(fecha.getMinutes()+""):("0" + fecha.getMinutes());
                if (Locale.getDefault().getDisplayLanguage().equals("español"))
                    mesDia = fecha.getDate() + "/" + (fecha.getMonth() + 1);
                else
                    mesDia = (fecha.getMonth() + 1) + "/" + fecha.getDate();
                formato = getString(R.string.header) + " " + data.group(2) + " " + getString(R.string.middle) +
                        " " + mesDia + " (" + fecha.getHours() + ":" + minutos + ")";
            }catch(Exception e){
                sc.findInLine("nombre='(.*)'&reserva=''&fecha='0'");
                data = sc.match();
                formato = getString(R.string.free);
            }
            builder = cursor.newRow();
            builder.add(j).add(data.group(1)).add(formato);
            sc.close();
        }
    }

    /**
     * Muestra una ventana emergente de espera e inicia la comunicación para la búsqueda.
     */
    private void empezar(){
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

        Todos todos = new Todos(this);
        String busqueda = buscar.getText().toString();
        String tipo = (nom.isChecked())?"usuario":"instrumento";
        todos.execute(busqueda, tipo);
    }

    /**
     * Clase encargada de gestionar la comunicación para obtener todos los instrumentos que existan
     * que cumplan ciertas condiciones.
     */
    private class Todos extends AsyncTask<String, Void, InputStream> {

        private HttpURLConnection http;
        private Context context;

        Todos(Context c){
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
                url = new URL(utiles.getDirBase() + "/instrumentos/todos.php?_query=" + utiles.pasarHexadecimal(params[0]) + "&_tipo=" + utiles.pasarHexadecimal(params[1]));
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
                MatrixCursor cursor = new MatrixCursor(new String[] {"_id", "Instrumento", "Datos"});
                if(datosEntrada.equals("Null\n")) {
                    vacio = true;
                    cursor.newRow().add(1).add(context.getString(R.string.res_empty)).add("");
                }else {
                    vacio = false;
                    descodificarLista(datosEntrada, cursor);
                }
                SimpleCursorAdapter adapter = new SimpleCursorAdapter(context, android.R.layout.simple_list_item_2, cursor, new String[] {"Instrumento", "Datos"}, new int[] {android.R.id.text1, android.R.id.text2});
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
