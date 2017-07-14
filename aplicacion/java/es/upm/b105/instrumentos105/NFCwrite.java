package paquete;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
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

/**
 * Created by miguelvp on 14/12/2016.
 * Clase encargada de gestionar la escritura de etiquetas.
 */

public class NFCwrite extends Activity{

    private EditText intInst;
    private TextView caracteres;
    private boolean escribir;
    private NfcAdapter adaptador;
    private AlertDialog cuadro;
    private int tipo;
    private Utils utiles;
    private boolean ventana;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.escribir_layout);
        intInst = (EditText) findViewById(R.id.Instrumento);
        caracteres = (TextView) findViewById(R.id.max);
        utiles = Utils.getSingleton();
        ventana = false;
    }

    @Override
    protected void onStart(){
        super.onStart();
        Intent intent = getIntent();
        Tag tag = intent.getParcelableExtra(Utils.TAG_EXTRA);
        Ndef ndef = Ndef.get(tag);
        boolean vacio = true;
        try {
            if (ndef != null) {
                ndef.connect();
                if (!ndef.isWritable()){
                    Toast.makeText(this, R.string.t_noW, Toast.LENGTH_LONG).show();
                    ndef.close();
                    finish();
                }
                tipo = 1;
                int maxNombre = ndef.getMaxSize();
                NdefMessage contiene = ndef.getNdefMessage();
                vacio = ((contiene == null) || (contiene.getRecords()[0].getTnf() == NdefRecord.TNF_EMPTY));
                String put = "(máx ";
                put = put.concat(Integer.toString(maxNombre-44)).concat(" caracteres)"); // 44 que es el numero de B que ocupa el AAR
                caracteres.setText(put);
                ndef.close();
            } else {
                NdefFormatable nform = NdefFormatable.get(tag);
                if (nform == null) {
                    finish();
                }
                tipo = 2;
            }
        } catch (Exception e){
            finish();
        }
        if (!vacio){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.importante);
            builder.setMessage(R.string.info_tag);
            builder.setPositiveButton(R.string.aceptar, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            builder.setNegativeButton(R.string.cancelar, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            builder.setCancelable(false);
            cuadro = builder.create();
            cuadro.show();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        if (cuadro != null){
            cuadro.dismiss();
        }
        if (escribir)
            desactivarEscribir();
    }

    @Override
    protected void onNewIntent(Intent intent){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null || !ni.isConnected()) {
            Toast.makeText(this, R.string.t_connect, Toast.LENGTH_SHORT).show();
            return;
        }
        if (escribir) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String nombre = intInst.getText().toString();
            NdefRecord grabar = null;
            try {
                grabar = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT,
                        null, nombre.getBytes("UTF-8"));
            } catch (Exception e){
                finish();
            }
            NdefMessage guardar = new NdefMessage(grabar,
                    NdefRecord.createApplicationRecord(paquete de la aplicaci�n));
            Ndef nuevo;
            NdefFormatable nuevo2;
            Anadir add = new Anadir();

            switch (tipo) {
                case 1:
                    try {
                        nuevo = Ndef.get(tag);
                        nuevo.connect();
                        nuevo.writeNdefMessage(guardar);
                        nuevo.close();
                        add.execute(nombre,utiles.getNombre());
                    } catch (Exception e) {
                        finish();
                    }
                    break;
                case 2:
                    try {
                        nuevo2 = NdefFormatable.get(tag);
                        nuevo2.connect();
                        nuevo2.format(guardar);
                        nuevo2.close();
                        add.execute(nombre,utiles.getNombre());
                    } catch (Exception e) {
                        finish();
                    }
                    break;
            }
            setResult(21);
            finish();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.tituloEscribir).setMessage(R.string.confirmarEscribir);
            builder.setPositiveButton(R.string.aceptar, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int i) {
                }
            });
            builder.show();
        }

    }

    /**
     * Comienza el proceso de comunicación mirando antes si el nombre es válido.
     * @param v Botón de enviar datos.
     */
    public void escribirDatos(View v){
        ventana = true;
        String nombre = intInst.getText().toString();
        if (!sonASCII(nombre)) {
            Toast.makeText(this, R.string.t_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.server_connection);
        builder.setView(new ProgressBar(this));
        builder.setMessage(R.string.check_name);
        builder.setNegativeButton(R.string.cancelar, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ventana = false;
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

        Existe add = new Existe();
        add.execute(intInst.getText().toString(), utiles.getNombre());
    }

    /**
     * Activa el foregroundDispatch para forzar la lectura de las etiquetas que podemos codificar
     * como NDEF.
     */
    private void activarEscribir() {
        PendingIntent pendiente = PendingIntent.getActivity(this.getApplicationContext(),
                0, new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter[] filtros = new IntentFilter[1];
        filtros[0] = new IntentFilter();
        filtros[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filtros[0].addCategory(Intent.CATEGORY_DEFAULT);
            /*
            filtros[1].addAction(NfcAdapter.ACTION_TECH_DISCOVERED);
            filtros[1].addCategory(Intent.CATEGORY_DEFAULT);
            */
        String[][] tecnologias = new String[2][1];
        tecnologias[0][0] = "android.nfc.tech.Ndef";
        tecnologias[1][0] = "android.nfc.tech.NdefFormatable";
        adaptador = NfcAdapter.getDefaultAdapter(getApplicationContext());
        adaptador.enableForegroundDispatch(this, pendiente, filtros, tecnologias);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.tituloActivar).setMessage(R.string.aviso);
        builder.setNegativeButton(R.string.cancelar, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int i) {
                escribir = false;
                desactivarEscribir();
            }
        });
        builder.setCancelable(false);
        cuadro = builder.create();
        cuadro.show();
    }

    /**
     * Desactiva el foregroundDispatch para no forzar la lectura del tag.
     */
    private void desactivarEscribir(){
        adaptador.disableForegroundDispatch(this);
    }

    /**
     * Muestra un mensaje de aviso para hacer saber que el nuevo tag sería un duplicado.
     */
    private void duplicado(){
        ventana = true;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.importante);
        builder.setMessage(R.string.duplicado);
        builder.setPositiveButton(R.string.aceptar, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ventana = false;
                escribir = true;
                activarEscribir();
            }
        });
        builder.setNegativeButton(R.string.cancelar, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ventana = false;
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                ventana = false;
            }
        });
        cuadro = builder.create();
        cuadro.show();
    }

    /**
     * Comprueba si la cadena de caracteres pasada está compuesta por caracteres ASCII válidos.
     * @param in Cadena de caracteres a comprobar.
     * @return <ul>True: cadena válida.</li><li>False: cadena inválida.</li></ul>
     */
    private boolean sonASCII(String in){
        char[] cadena = in.toCharArray();
        for(char c : cadena) {
            if(((int)c)>126 || ((int)c)==39 || ((int)c)==34 || ((int)c)<32)
                return false;
        }
        return true;
    }

    /**
     * Clase encargada de gestionar la comunicación para añadir un instrumento.
     */
    private class Anadir extends AsyncTask<String, Void, InputStream> {

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
                url = new URL(utiles.getDirBase() + "/instrumentos/anadir.php");
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

            if (escribir && stream != null) {
                String datosEntrada = utiles.leerDatos(stream);
                if (datosEntrada.equals("OK\n")){
                    Toast.makeText(getApplicationContext(), R.string.t_create, Toast.LENGTH_SHORT).show();
                } else if (datosEntrada.equals("Duplicado\n")) {
                    Toast.makeText(getApplicationContext(), R.string.t_duplicate, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.t_noCreate, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), R.string.t_noAdd, Toast.LENGTH_SHORT).show();
            }
            http.disconnect();
        }
    }

    /**
     * Clase encargada de gestionar la comunicación para comprobar la existencia de un instrumento.
     */
    private class Existe extends AsyncTask<String, Void, InputStream> {

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
                url = new URL(utiles.getDirBase() + "/instrumentos/existe.php");
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

            if (ventana && stream != null) {
                String datosEntrada = utiles.leerDatos(stream);
                cuadro.dismiss();
                ventana = false;
                if (datosEntrada.equals("False\n")){
                    escribir = true;
                    activarEscribir();
                }else if (datosEntrada.equals("Duplicado\n")) {
                    duplicado();
                }else if (datosEntrada.equals("Failed connection")) {
                    Toast.makeText(getApplicationContext(), R.string.t_user, Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(getApplicationContext(), R.string.t_yetExist, Toast.LENGTH_SHORT).show();
                }
            } else {
                cuadro.dismiss();
            }
            http.disconnect();
        }
    }

}
