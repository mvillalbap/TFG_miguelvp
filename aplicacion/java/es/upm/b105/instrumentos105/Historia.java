package paquete;

import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
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
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by miguelvp on 27/06/2017.
 */

public class Historia extends AppCompatActivity {

    private EditText newInc;
    private TextView oldInc;
    private RadioButton sin, add, cambios;
    private Button send;
    private int pulsado;
    private Utils utiles;
    private Incidencias inc;
    private AlertDialog cuadro;
    private String reserva;
    private String fecha;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_historia);
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null || !ni.isConnected()) {
            Toast.makeText(getApplicationContext(), R.string.t_connect, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            newInc = (EditText) findViewById(R.id.newIncidencias);
            TextView nombre = (TextView) findViewById(R.id.nombre);
            oldInc = (TextView) findViewById(R.id.incidenciasAntiguas);
            sin = (RadioButton) findViewById(R.id.sin);
            add = (RadioButton) findViewById(R.id.add);
            cambios = (RadioButton) findViewById(R.id.cambios);
            send = (Button) findViewById(R.id.crearHistoria);
            newInc.setEnabled(false);
            newInc.setBackgroundColor(getResources().getColor(R.color.colorDisable));
            pulsado = 4;
            inc = new Incidencias();
            send.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                    NetworkInfo ni = cm.getActiveNetworkInfo();
                    if (ni == null || !ni.isConnected()) {
                        Toast.makeText(getApplicationContext(), R.string.t_connect, Toast.LENGTH_SHORT).show();
                    } else {
                        Registrar reg = new Registrar(pulsado);
                        reg.execute(utiles.getNombre(), reserva, fecha, newInc.getText().toString());
                    }
                }
            });
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.server_connection);
            builder.setView(new ProgressBar(this));
            builder.setMessage(R.string.t_rec_data);
            builder.setNegativeButton(R.string.cancelar, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    inc.setActivo(false);
                    finish();
                }
            });
            builder.setCancelable(false);
            cuadro = builder.create();
            cuadro.show();
            try {
                reserva = getIntent().getExtras().get(Utils.USER_SESSION).toString();
                fecha = getIntent().getExtras().get(Utils.FECHA_INC).toString();
                Date fecha = new Date(Long.valueOf(this.fecha) * 1000);
                String mesDia;
                String minutos = (fecha.getMinutes() > 9)?(fecha.getMinutes()+""):("0" + fecha.getMinutes());
                if (Locale.getDefault().getDisplayLanguage().equals("español"))
                    mesDia = fecha.getDate() + "/" + (fecha.getMonth() + 1);
                else
                    mesDia = (fecha.getMonth() + 1) + "/" + fecha.getDate();
                nombre.setText(reserva + " (" + mesDia + " " + fecha.getHours() + ":" + minutos + ")");
                inc.execute(reserva);
            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.malNotif), Toast.LENGTH_LONG).show();
                finish();
            } finally {
                utiles = Utils.getSingleton();
                utiles.openData(this);
            }
        }
    }

    public void radioPulsado(View v){
        pulsado = (sin.isChecked() || cambios.isChecked())?
                ((sin.isChecked())?1:4):((add.isChecked())?2:3);
        newInc.setEnabled(pulsado != 1 && pulsado != 4);
        if(pulsado != 1 && pulsado != 4){
            newInc.setBackgroundColor(getResources().getColor(R.color.colorPrimaryLight));
        }else{
            newInc.setBackgroundColor(getResources().getColor(R.color.colorDisable));
        }
    }

    private String getIncidencias(String in){
        Pattern pattern = Pattern.compile("incidencias='([%\\w]+)'\\s");
        Matcher matcher = pattern.matcher(in);
        String salida = null;
        char[] devolver = new char[1];
        if(matcher.find()) {
            try {
                salida = matcher.group(1);
                devolver = new char[salida.length()/3];
                for(int i = 0; i < salida.length(); i += 3){
                    int sal = Integer.parseInt(salida.substring(i+1,i+3),16);
                    devolver[i/3] = (char) sal;
                }
            } catch (Exception e){}
        }else{
            devolver = new char[1];
            devolver[0] = ' ';
        }
        return new String(devolver);
    }

    /**
     * Clase encargada de gestionar la comunicación para obtener todos los instrumentos que existan
     * que cumplan ciertas condiciones.
     */
    private class Incidencias extends AsyncTask<String, Void, InputStream> {

        private HttpURLConnection http;
        private boolean activo;

        Incidencias(){
            activo = true;
        }

        void setActivo(boolean activo){
            this.activo = activo;
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
                url = new URL(utiles.getDirBase() + "/instrumentos/ultima.php?_query=" + utiles.pasarHexadecimal(params[0]));
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

            if (stream != null && activo) {
                String datosEntrada = utiles.leerDatos(stream);
                if(!datosEntrada.equals("Null\n")) {
                    oldInc.setText(getIncidencias(datosEntrada));
                }
            } else {
                Toast.makeText(getApplicationContext(), R.string.t_server, Toast.LENGTH_LONG).show();
            }
            http.disconnect();
            cuadro.dismiss();
        }
    }

    private class Registrar extends AsyncTask<String, Void, InputStream> {

        private int tipo;
        private String inc, user, fecha, res;
        private HttpURLConnection http;

        public Registrar(int tipo){
            this.tipo = tipo;
        }

        @Override
        protected InputStream doInBackground(String... params){
            user = params[0];
            res = params[1];
            fecha = params[2];
            if(tipo != 1){
                inc = params[3];
            }
            URL url;
            URLConnection urlC;
            InputStream in = null;
            OutputStream out;
            String mandar;
            try {
                url = new URL(utiles.getDirBase() + "/instrumentos/registrar.php");
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
                mandar = "usuario=" + utiles.pasarHexadecimal(user) + "&reserva=" + utiles.pasarHexadecimal(res)
                        + "&fecha=" + fecha + "&tipo=" + Integer.valueOf(tipo).toString();
                if(tipo != 1){
                    mandar = mandar.concat("&incidencias=" + utiles.pasarHexadecimal(inc));
                }
                writer.write(mandar);
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
                    Toast.makeText(getApplicationContext(), R.string.t_saveReg, Toast.LENGTH_SHORT).show();
                    utiles.borrarRegistro(res, Long.valueOf(fecha), getApplicationContext());
                    finish();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.t_eraseReg, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), R.string.t_server, Toast.LENGTH_SHORT).show();
            }
            http.disconnect();
        }
    }


}
