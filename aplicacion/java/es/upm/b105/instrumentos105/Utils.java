package paquete;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by miguelvp on 26/01/2017.
 * Clase con patrón singleton para gestionar variables que puedan acceder desde varias actividades.
 */

public class Utils {

    private static Utils singleton = new Utils();

    private static final String DIRECCION_BASE = "DIR_BASE";

    // Códigos tras los lanzamientos de actividades y devolver resultados
    public static final int SESION = 10;
    public static final int SESION_OK = 11;
    public static final int SESION_SUDO = 12;
    public static final int ESCRIBIR = 20;
    public static final int ESCRIBIR_OK = 21;

    // Códigos de éxito o error a la hora de borrar y guardar
    public static final int ERROR = -10;
    public static final int EXITO = 10;

    // Nombres en las notificaciones
    public static final String TAG_RESERVAS = "RESERVAS";
    public static final String TAG_PETICIONES = "PETICIONES";
    public static final String TAG_HISTORIA = "HISTORIA";

    // Nombres para recuperar objetos
    public static final String USER_SESSION = "NOMBRE";
    public static final String TAG_EXTRA = "TAG";
    public static final String FECHA_INC = "FECHA";

    // Valores para la codificación RSA
    public static final int CLAVE_PUBLICA = XX;
    public static final int MODULO = XX;

    private boolean logged = false;
    private boolean borrado = false;
    private boolean permisos = false;

    private String direccion = "direccion de nuestro servidor";

    private AlertDialog cuadro;
    private ReservasOpener data;
    private PeticionesOpener pet;
    private HistoriaOpener hist;
    private ArrayList<Integer> ocupados = new ArrayList<>();
    private AlarmManager alarmManager;

    private static final String RESERVAS_NAME = "Instrumentos105DB";
    private static final String PETICIONES_NAME = "Instrumentos105peticiones";
    private static final String HISTORIA_NAME = "Instrumentos105historia";
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_ID = "id";
    private static final String DATABASE_RESERVA = "nombre";
    private static final String DATABASE_FECHA = "fecha";
    private static final String DATABASE_INCIDENCIAS_NUEVAS = "incidenciasNuevas";
    private static final int ID_USER = 1;
    private static final int ID_SUDO = -1;

    private PendingIntent alarmEjec;
    private final int SEC_ALARMA = 300;

    private ReentrantLock dataLock = new ReentrantLock();
    private ReentrantLock petLock = new ReentrantLock();
    private ReentrantLock histLock = new ReentrantLock();

    /**
     * Realiza la actualización de los instrumentos guardados.
     * @param c Contexto de trabajo.
     */
    public void actualizar(Context c){
        borrarRegistros(c);
        Actualizar act = new Actualizar(c);
        act.execute(getNombre());
    }

    /**
     * Borra un instrumento de la base de datos de las reservas.
     * @param nombre Nombre del instrumento.
     * @return Código de error o de éxito en la operación.
     */
    public int borrar(String nombre){
        dataLock.lock();
        try {
            SQLiteDatabase db = data.getWritableDatabase();
            Cursor cursor = db.query(RESERVAS_NAME, new String[]{DATABASE_ID},
                    DATABASE_RESERVA + "='" + nombre + "'", null, null, null, null);
            if (!cursor.moveToFirst()) {
                cursor.close();
                db.close();
                return ERROR;
            }
            cursor.close();
            db.delete(RESERVAS_NAME, DATABASE_RESERVA + "='" + nombre + "'", null);
            db.close();
            actualizaOcupados();
        }finally {
            dataLock.unlock();
        }
        return EXITO;
    }

    /**
     * Borra un instrumento de la base de datos de avisos. Lleva incorporada la gestión del
     * temporizador para cancelar en caso de que no haya más instrumentos en la base de datos.
     * @param nombre Nombre del instrumento a borrar.
     */
    public void borrarPeticion(String nombre){
        petLock.lock();
        try {
            SQLiteDatabase db = pet.getWritableDatabase();
            db.delete(PETICIONES_NAME, DATABASE_RESERVA + "='" + nombre + "'", null);
            db.close();
            actualizaOcupados();
            HashMap<String, Integer> petGuardadas = getPeticiones();
            if (petGuardadas == null)
                alarmManager.cancel(alarmEjec);
        }finally{
            petLock.unlock();
        }
    }

    /**
     * Borra toda la tabla de peticiones.
     */
    public void borrarPeticiones(){
        petLock.lock();
        try {
            SQLiteDatabase db = pet.getWritableDatabase();
            db.delete(PETICIONES_NAME, null, null);
            db.close();
            actualizaOcupados();
            alarmManager.cancel(alarmEjec);
        }finally{
            petLock.unlock();
        }
    }

    public void borrarRegistro(String nombre, long fechaRegistro, Context c){
        histLock.lock();
        try{
            SQLiteDatabase db = hist.getWritableDatabase();
            Cursor cursor = db.query(HISTORIA_NAME, new String[]{DATABASE_ID},
                    DATABASE_RESERVA + "='" + nombre + "' AND " +
                    "(" + DATABASE_FECHA + ">=" + (fechaRegistro - 60*60) + " AND " + DATABASE_FECHA + "<="+ (fechaRegistro + 60*60) + ")",
                    null, null, null, null);
            if (!cursor.moveToFirst()) {
                cursor.close();
                db.close();
                return;
            }
            do {
                int index = cursor.getInt(0);
                db.delete(HISTORIA_NAME, DATABASE_FECHA + "=" + fechaRegistro, null);
                NotificationManager manNotif = (NotificationManager) c.getSystemService(NOTIFICATION_SERVICE);
                manNotif.cancel(TAG_HISTORIA, index);
            } while(cursor.moveToNext());
            db.close();
        }finally{
            histLock.unlock();
        }
    }

    /**
     * Borra toda la tabla de registros.
     */
    public void borrarRegistros(Context c){
        histLock.lock();
        try{
            SQLiteDatabase db = hist.getWritableDatabase();
            NotificationManager manNotif = (NotificationManager) c.getSystemService(NOTIFICATION_SERVICE);
            Cursor cursor = db.query(HISTORIA_NAME, new String[]{DATABASE_ID},
                    null, null, null, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    int index = cursor.getInt(0);
                    manNotif.cancel(TAG_HISTORIA, cursor.getInt(0));
                } while (cursor.moveToNext());
            }
            cursor.close();
            db.delete(HISTORIA_NAME, null, null);
            db.close();
        }finally{
            histLock.unlock();
        }
    }

    /**
     * Consulta si existe el instrumento en la base de datos de las reservas.
     * @param nombre Nombre del instrumento.
     * @return Id del instrumento en la tabla para poder eliminar la notificación creada.
     */
    public int contiene(String nombre){
        int aux;
        dataLock.lock();
        try{
            SQLiteDatabase db = data.getReadableDatabase();
            Cursor cursor = db.query(RESERVAS_NAME, new String[]{DATABASE_ID},
                    DATABASE_RESERVA + "='" + nombre + "'", null, null, null, null);
            if (!cursor.moveToFirst()) {
                cursor.close();
                db.close();
                return 0;
            }
            aux = cursor.getInt(0);
            cursor.close();
            db.close();
        }finally{
            dataLock.unlock();
        }
        return aux;
    }

    /**
     * Codifica con RSA la cadena de caracteres pasada como parámetro con una clave pública y el
     * producto de clave privada y pública (mod). Además añade un relleno de caracteres entre medias
     * para mejorar la encriptación.
     * @param cadena Cadena de caracteres a codificar.
     * @param pubKey Clave pública a utilizar.
     * @param mod Producto de clave pública por clave privada.
     * @return El string codificado que representa la cadena de caracteres pasado como parámetro.
     */
    public String codificarRSArelleno(char[] cadena, int pubKey, int mod){
        String dev = "";
        int sol, temp;
        int acumulado = 0;
        boolean cero;
        for(int i = 0; i < cadena.length; i++){
            sol = 1;
            for(int j = 1; j <= pubKey; j++){
                sol = (sol * (int)(cadena[i])) % mod;
            }
            for(int k = 0; k < 4; k++){
                temp = (sol/((int)(Math.pow(10,3-k))))%10;
                acumulado += temp;
                dev = dev.concat(String.valueOf(temp));
            }
            acumulado = (acumulado * acumulado) % 9507;
            cero = true;
            for(int q = 0; q < 4; q++){
                temp = (acumulado/((int)(Math.pow(10,3-q))))%10;
                if(!(cero && temp == 0)){
                    cero = false;
                    dev = dev.concat(String.valueOf(temp));
                }
            }
        }
        return dev;
    }

    /**
     * Crea las notificaciones correspondientes a los instrumentos guardados en la base de datos de
     * las reservas.
     * @param c Contexto sobre el que se está trabajando.
     */
    public void creaNotif(Context c){
        dataLock.lock();
        try {
            SQLiteDatabase db = data.getReadableDatabase();
            NotificationManager manNotif = (NotificationManager) c.getSystemService(NOTIFICATION_SERVICE);
            Cursor cursor = db.query(RESERVAS_NAME, new String[]{DATABASE_ID, DATABASE_RESERVA},
                    "NOT ( " + DATABASE_ID + "=? OR " +
                            DATABASE_ID + "=? )",
                    new String[]{(Integer.valueOf(ID_USER)).toString(), (Integer.valueOf(ID_SUDO)).toString()},
                    null, null, null);
            if (!cursor.moveToFirst()) {
                cursor.close();
                return;
            }
            do {
                Notification.Builder builder = new Notification.Builder(c);
                builder.setContentTitle(cursor.getString(1));
                builder.setContentText(c.getString(R.string.notif_reserva));
                builder.setSmallIcon(R.drawable.logonotif);
                builder.setAutoCancel(true);
                builder.setOngoing(true);
                manNotif.notify(TAG_RESERVAS, cursor.getInt(0), builder.build());
            } while (cursor.moveToNext());
            cursor.close();
            db.close();
        }finally {
            dataLock.unlock();
        }
    }

    /**
     * Consultar si se está en el proceso de borrado de una etiqueta.
     * @return <ul><li>True: se está borrando.</li><li>False: no se está borrando.</li></ul>
     */
    public boolean getBorrado() {
        return borrado;
    }

    /**
     * Consultar la dirección URI al que se tiene que realizar la comunicación.
     * @return Dirección URI al que se comunica el sistema.
     */
    public String getDirBase(){
        return direccion;
    }

    /**
     * Consultar el id con el que está guardado el instrumento indicado por parámetro.
     * @param nombre Nombre del instrumento del que obtener el id.
     * @return Id del instrumento consultado.
     */
    public int getIdPeticion(String nombre){
        int devolver;
        petLock.lock();
        try{
            SQLiteDatabase db = pet.getReadableDatabase();
            Cursor cursor = db.query(PETICIONES_NAME, new String[]{DATABASE_ID},
                    DATABASE_RESERVA + "='" + nombre + "'", null, null, null, null);
            if (!cursor.moveToFirst()) {
                cursor.close();
                db.close();
                devolver = -1;
            }else {
                devolver = cursor.getInt(0);
                cursor.close();
                db.close();
            }
        }finally {
            petLock.unlock();
        }
        return devolver;
    }

    /**
     * Consultar el nombre de usuario que se encuentra conectado en el sistema.
     * @return Nombre del usuario conectado.
     */
    public String getNombre(){
        String aux;
        dataLock.lock();
        try {
            SQLiteDatabase db = data.getReadableDatabase();
            Cursor cursor = db.query(RESERVAS_NAME, new String[]{DATABASE_RESERVA},
                    DATABASE_ID + "=" + (Integer.valueOf(1)).toString(), null, null, null, null);
            if (!cursor.moveToFirst()) {
                cursor.close();
                db.close();
                aux = null;
            }else {
                aux = cursor.getString(0);
                cursor.close();
                db.close();
            }
        }finally {
            dataLock.unlock();
        }
        return aux;
    }

    /**
     * Consultar todos los instrumentos guardados de lo que generar avisos. Se devuelven sobre
     * un mapa donde se indica el nombre y su id.
     * @return Mapa con los instrumentos y sus id.
     */
    public HashMap<String, Integer> getPeticiones(){
        HashMap<String, Integer> devolver = new HashMap<>();
        petLock.lock();
        try {
            SQLiteDatabase db = pet.getReadableDatabase();
            Cursor cursor = db.query(PETICIONES_NAME, new String[]{DATABASE_ID, DATABASE_RESERVA},
                    null, null, null, null, null);
            if (!cursor.moveToFirst()) {
                cursor.close();
                db.close();
                devolver = null;
            } else {
                do {
                    devolver.put(cursor.getString(1), cursor.getInt(0));
                } while (cursor.moveToNext());
                cursor.close();
                db.close();
            }
        }finally {
            petLock.unlock();
        }
        return devolver;
    }

    /**
     * Obtención del singleton para su uso por todas las actividades.
     * @return El singleton para tener acceso a la información.
     */
    public static Utils getSingleton(){
        return singleton;
    }

    /**
     * Guarda el instrumento indicado en la base de datos de las reservas.
     * @param nombre Nombre del instrumento a guardar.
     * @return El id con el que se ha guardado en la tabla.
     */
    public int guardar(String nombre){
        int pos;
        dataLock.lock();
        try {
            SQLiteDatabase db = data.getWritableDatabase();
            Cursor cursor = db.query(RESERVAS_NAME, new String[]{DATABASE_ID},
                    DATABASE_RESERVA + "='" + nombre + "'", null, null, null, null);
            if (cursor.moveToFirst()) {
                cursor.close();
                db.close();
                pos = ERROR;
            } else {
                cursor.close();
                pos = Math.abs((new Random(System.currentTimeMillis())).nextInt()) % 1024 + 2;
                while (ocupados.contains(pos)) {
                    pos = Math.abs((new Random(System.currentTimeMillis())).nextInt()) % 1024 + 2;
                }
                ContentValues cv = new ContentValues(2);
                cv.put(DATABASE_ID, pos);
                cv.put(DATABASE_RESERVA, nombre);
                db.insert(RESERVAS_NAME, null, cv);
                db.close();
                ocupados.add(pos);
            }
        }finally {
            dataLock.unlock();
        }
        return pos;
    }

    public int guardarRegistro(String nombre, long fecha, Context c){
        int pos;
        histLock.lock();
        try {
            SQLiteDatabase db = hist.getWritableDatabase();
            ContentValues cv = new ContentValues(2);
            cv.put(DATABASE_RESERVA, nombre);
            cv.put(DATABASE_FECHA, fecha);
            pos = (int) db.insert(HISTORIA_NAME, null, cv);
            db.close();
            Intent notif = new Intent(c, Historia.class);
            notif.putExtra(Utils.USER_SESSION, nombre);
            notif.putExtra(Utils.FECHA_INC, String.valueOf(fecha));
            NotificationManager manNotif = (NotificationManager) c.getSystemService(NOTIFICATION_SERVICE);
            Notification.Builder builder = new Notification.Builder(c);
            builder.setContentTitle(nombre);
            builder.setContentText(c.getString(R.string.notifIncidencias));
            builder.setSmallIcon(R.drawable.logonotif);
            builder.setAutoCancel(false);
            builder.setOngoing(true);
            builder.setContentIntent(PendingIntent.getActivity(c, pos, notif, PendingIntent.FLAG_CANCEL_CURRENT));
            manNotif.notify(TAG_HISTORIA, pos, builder.build());
        }finally {
            histLock.unlock();
        }
        return pos;
    }

    /**
     * Consultar si el usuario se encuentra conectado.
     * @return <ul><li>True: hay un usuario conectado.</li><li>False: no hay un usuario conectado.</li></ul>
     */
    public boolean isLogged(){
        return logged;
    }

    /**
     * Consultar si el instrumento pasado se encuentra dentro de la base de datos de avisos.
     * @param nombre Nombre del instrumento a consultar.
     * @return <ul><li>True: el instrumento se encuentra en la tabla.</li>
     * <li>False: el instrumento no está en la tabla.</li></ul>
     */
    public boolean isPedido(String nombre){
        boolean devolver = true;
        petLock.lock();
        try {
            SQLiteDatabase db = pet.getReadableDatabase();
            Cursor cursor = db.query(PETICIONES_NAME, new String[]{DATABASE_RESERVA},
                    DATABASE_RESERVA + "='" + nombre + "'", null, null, null, null);
            if (!cursor.moveToFirst()) {
                devolver = false;
            }
            cursor.close();
            db.close();
        }finally {
            petLock.unlock();
        }
        return devolver;

    }

    /**
     * Consultar si el usuario conectado tiene permisos.
     * @return <ul><li>True: el usuario conectado tiene permisos.</li>
     * <li>False: el usuario conectado no tiene permisos.</li></ul>
     */
    public boolean isSudo(){
        return permisos;
    }

    /**
     * Descodifica la información pasada en el stream para conocer el tipo de respuesta que ha recibido.
     * @param in Stream a descodificar.
     * @return String ya descodificado con la respuesta.
     */
    public String leerDatos(InputStream in) {
        BufferedReader lector = new BufferedReader(new InputStreamReader(in));
        String linea;
        String devolver = "";
        try {
            while ((linea = lector.readLine()) != null) {
                devolver = devolver.concat(linea + '\n');
            }
            lector.close();
        }catch (Exception e) {
            return null;
        }
        return devolver;
    }

    /**
     * Carga los datos del singleton que comparten las actividades.
     * @param c Contexto en el que se está trabajando.
     */
    public void openData(Context c){
        petLock.lock();
        dataLock.lock();
        histLock.lock();
        try {
            pet = new PeticionesOpener(c);
            data = new ReservasOpener(c);
            hist = new HistoriaOpener(c);
            SQLiteDatabase db = data.getReadableDatabase();
            Cursor cursor = db.query(RESERVAS_NAME, new String[]{DATABASE_ID},
                    DATABASE_ID + "=" + (Integer.valueOf(ID_USER)).toString(), null, null, null, null);
            logged = cursor.moveToFirst();
            cursor.close();
            cursor = db.query(RESERVAS_NAME, new String[]{DATABASE_ID},
                    DATABASE_ID + "=" + (Integer.valueOf(ID_SUDO)).toString(), null, null, null, null);
            permisos = cursor.moveToFirst();
            cursor.close();
            db.close();
            String dir = c.getSharedPreferences(DIRECCION_BASE, 0).getString(DIRECCION_BASE, null);
            if (dir != null)
                direccion = dir;
            actualizaOcupados();
            alarmManager = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            alarmEjec = PendingIntent.getBroadcast(c, 0, new Intent(c, Peticiones.class), 0);
        }finally {
            petLock.unlock();
            dataLock.unlock();
            histLock.unlock();
        }
    }

    /**
     * Devuelve el string pasado como parámetro como su representación en hexadecimal de cada uno
     * de los caracteres para que pueda ser enviado correctamente en un paquete HTTP.
     * @param in String a codificar.
     * @return String codificado.
     */
    public String pasarHexadecimal(String in){
        String devolver = "";
        int longitud = in.length();
        for(int i = 0; i < longitud; i++){
            int dec = (int) in.charAt(i);
            int hex1 = dec/16;
            int hex2 = dec%16;
            if(hex1>9)
                devolver = devolver.concat("%" + (char)(hex1+55));
            else
                devolver = devolver.concat("%" + (char)(hex1+48));
            if(hex2>9)
                devolver = devolver.concat("" + (char)(hex2+55));
            else
                devolver = devolver.concat("" + (char)(hex2+48));
        }
        return devolver;
    }

    /**
     * Establece si se encuentra en el proceso de borrado.
     * @param borrar <ul><li>True: si se encuentra borrando.</li>
     *               <li>False: si no se encuentra borrando.</li></ul>
     */
    public void setBorrado(boolean borrar) {
        borrado = borrar;
    }

    /**
     * Establece la dirección URI con la que realizar la comunicación.
     * @param in Dirección nueva.
     */
    public void setDirBase(String in){
        direccion = in;
    }

    /**
     * Establece el nombre del usuario conectado (sin permisos) e inicia la recuperación de sus reservas.
     * @param conectado Nombre del usuario (sin permisos).
     * @param c Contexto en el que se está trabajando.
     */
    public void setLogged(String conectado, Context c){
        dataLock.lock();
        try {
            SQLiteDatabase db = data.getWritableDatabase();
            if (conectado == null) {
                logged = false;
                permisos = false;
                borraNotif(db, c);
                db.delete(RESERVAS_NAME, null, null);
                ocupados = new ArrayList<>();
                db.close();
                return;
            }
            ContentValues cv = new ContentValues(2);
            cv.put(DATABASE_ID, ID_USER);
            cv.put(DATABASE_RESERVA, conectado);
            db.insert(RESERVAS_NAME, null, cv);
            db.close();
            SharedPreferences.Editor editor = c.getSharedPreferences(DIRECCION_BASE, 0).edit();
            editor.putString(DIRECCION_BASE, direccion);
            editor.apply();
            AlertDialog.Builder builder = new AlertDialog.Builder(c);
            builder.setTitle(R.string.server_connection);
            builder.setView(new ProgressBar(c));
            builder.setMessage(R.string.t_rec_data);
            cuadro = builder.create();
            cuadro.show();
            logged = true;
            ocupados.add(ID_USER);
        }finally {
            dataLock.unlock();
        }
        Recuperar rec = new Recuperar(c);
        rec.execute(conectado);
    }

    /**
     * Guarda un instrumento en la base de datos de avisos.
     * @param nombre Nombre del instrumento a guardar.
     */
    public void setPeticion(String nombre){
        HashMap<String, Integer> petGuardadas = getPeticiones();
        ArrayList<Integer> ids;
        if(petGuardadas != null)
            ids = new ArrayList<>(getPeticiones().values());
        else
            ids = new ArrayList<>();
        petLock.lock();
        try {
            SQLiteDatabase db = pet.getWritableDatabase();
            int pos = Math.abs((new Random(System.currentTimeMillis())).nextInt()) % 1024 + 1;
            while (ids.contains(pos)) {
                pos = Math.abs((new Random(System.currentTimeMillis())).nextInt()) % 1024 + 1;
            }
            ContentValues cv = new ContentValues(2);
            cv.put(DATABASE_ID, pos);
            cv.put(DATABASE_RESERVA, nombre);
            db.insert(PETICIONES_NAME, null, cv);
            db.close();
            if (ids.size() == 0) {
                alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + SEC_ALARMA * 1000,
                        SEC_ALARMA * 1000, alarmEjec);
            }
        }finally {
            petLock.unlock();
        }
    }

    /**
     * Establece el nombre del usuario conectado (con permisos) e inicia la recuperación de sus reservas.
     * @param conectado Nombre del usuario (con permisos).
     * @param c Contexto en el que se está trabajando.
     */
    public void setSudo(String conectado, Context c){
        dataLock.lock();
        try {
            SQLiteDatabase db = data.getWritableDatabase();
            if (conectado == null) {
                logged = false;
                permisos = false;
                borraNotif(db, c);
                db.delete(RESERVAS_NAME, null, null);
                ocupados = new ArrayList<>();
                db.close();
                return;
            }
            ContentValues cv = new ContentValues(2);
            cv.put(DATABASE_ID, ID_USER);
            cv.put(DATABASE_RESERVA, conectado);
            db.insert(RESERVAS_NAME, null, cv);
            cv.put(DATABASE_ID, ID_SUDO);
            cv.put(DATABASE_RESERVA, conectado);
            db.insert(RESERVAS_NAME, null, cv);
            db.close();
            SharedPreferences.Editor editor = c.getSharedPreferences(DIRECCION_BASE, 0).edit();
            editor.putString(DIRECCION_BASE, direccion);
            editor.apply();
            AlertDialog.Builder builder = new AlertDialog.Builder(c);
            builder.setTitle(R.string.server_connection);
            builder.setView(new ProgressBar(c));
            builder.setMessage(R.string.t_rec_data);
            cuadro = builder.create();
            cuadro.show();
            logged = true;
            permisos = true;
            ocupados.add(ID_USER);
            ocupados.add(ID_SUDO);
        }finally {
            dataLock.unlock();
        }
        Recuperar rec = new Recuperar(c);
        rec.execute(conectado);
    }

    /**
     * Actualiza la lista de ids ocupados en la tabla de reservas.
     */
    private void actualizaOcupados() {
        dataLock.lock();
        try {
            SQLiteDatabase db = data.getReadableDatabase();
            Cursor cursor = db.query(RESERVAS_NAME, new String[]{DATABASE_ID},
                    null, null, null, null, null);
            ocupados = new ArrayList<>();
            while (cursor.moveToNext()) {
                Integer ocup = cursor.getInt(0);
                ocupados.add(ocup);
            }
            cursor.close();
            db.close();
        }finally {
            dataLock.unlock();
        }
    }

    /**
     * Borra todos los instrumentos de la base de datos de las reservas.
     * @param c Contexto en el que se está trabajando.
     */
    private void borraInst(Context c){
        dataLock.lock();
        try {
            SQLiteDatabase db = data.getWritableDatabase();
            NotificationManager manNotif = (NotificationManager) c.getSystemService(NOTIFICATION_SERVICE);
            Cursor cursor = db.query(RESERVAS_NAME, new String[]{DATABASE_ID},
                    "NOT ( " + DATABASE_ID + "=? OR " +
                            DATABASE_ID + "=? )",
                    new String[]{(Integer.valueOf(ID_USER)).toString(), (Integer.valueOf(ID_SUDO)).toString()},
                    null, null, null);
            if (!cursor.moveToFirst()) {
                cursor.close();
                db.close();
                return;
            }
            do {
                manNotif.cancel(TAG_RESERVAS, cursor.getInt(0));
                db.delete(RESERVAS_NAME, DATABASE_ID + "='" + cursor.getInt(0) + "'", null);
            } while (cursor.moveToNext());
            cursor.close();
            db.close();
            actualizaOcupados();
        }finally {
            dataLock.unlock();
        }
    }

    /**
     * Borra todas las notificaciones asociadas a la base de datos pasada como parámetro.
     * @param db Base de datos a utilizar.
     * @param c Contexto en el que se está trabajando.
     */
    private void borraNotif(SQLiteDatabase db, Context c){
        NotificationManager manNotif = (NotificationManager) c.getSystemService(NOTIFICATION_SERVICE);
        Cursor cursor = db.query(RESERVAS_NAME, new String[]{DATABASE_ID, DATABASE_RESERVA},
                "NOT ( " + DATABASE_ID + "=? OR " +
                        DATABASE_ID + "=? )",
                new String[]{(Integer.valueOf(ID_USER)).toString(), (Integer.valueOf(ID_SUDO)).toString()},
                null, null, null);
        if (!cursor.moveToFirst()) {
            cursor.close();
            return;
        }
        do {
            manNotif.cancel(TAG_RESERVAS, cursor.getInt(0));
        } while(cursor.moveToNext());
        cursor.close();
    }

    /**
     * Descodifica los instrumentos recibidos en una sola tira de instrumentos en un mapa que contiene
     * el nombre del instrumento y la fecha desde que lleva reservado.
     * @param in Tira de valores de los instrumentos.
     * @return Mapa de nombres de instrumentos y las fechas de sus reservas.
     */
    private TreeMap<String, Long> descodificarInst(String in){
        TreeMap<String, Long> devolver = new TreeMap<>();
        String[] lineas = in.split("\n");
        Scanner sc;
        for (int j = 0; j < lineas.length; ++j){
            sc = new Scanner(lineas[j]);
            try {
                sc.findInLine("nombre='(.*)'&fecha='(\\d+)'");
                MatchResult data = sc.match();
                devolver.put(data.group(1), Long.valueOf(data.group(2)));
            }catch (Exception e){
            }
            sc.close();
        }
        return devolver;
    }

    /**
     * Descodifica los instrumentos recibidos en una sola tira de instrumentos en un mapa que contiene
     * el nombre del instrumento y la fecha desde que lleva reservado.
     * @param in Tira de valores de los instrumentos.
     * @return Mapa de nombres de instrumentos y las fechas de sus reservas.
     */
    private TreeMap<Long, String> descodificarReg(String in){
        TreeMap<Long, String> devolver = new TreeMap<>();
        String[] lineas = in.split("\n");
        Scanner sc;
        for (int j = 0; j < lineas.length; ++j){
            sc = new Scanner(lineas[j]);
            try {
                sc.findInLine("registro='(.*)'&fecha='(\\d+)'");
                MatchResult data = sc.match();
                devolver.put(Long.valueOf(data.group(2)), data.group(1));
            }catch (Exception e){
            }
            sc.close();
        }
        return devolver;
    }

    /**
     * Clase encargada de gestionar la comunicación y actualizar los instrumentos y sus notificaciones.
     */
    private class Actualizar extends AsyncTask<String, Void, InputStream> {

        private Context context;
        private String user;
        private HttpURLConnection http;

        protected Actualizar(Context c){
            context = c;
        }

        /**
         * Inicia la comunicación con el servidor.
         * @param params Recibe como parámetro el nombre de usuario.
         * @return El stream recibido del servidor para su descodificación.
         */
        @Override
        protected InputStream doInBackground(String... params) {
            user = params[0];
            URL url;
            URLConnection urlC;
            InputStream in = null;
            OutputStream out;
            try {
                url = new URL(getDirBase() + "/instrumentos/recuperar.php");
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
                writer.write("usuario=" + pasarHexadecimal(user));
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
                String datosEntrada = leerDatos(stream);

                if (datosEntrada.equals("Null\n")) {
                    borraInst(context);
                } else if (datosEntrada.equals("Failed connection")) {
                    Toast.makeText(context, R.string.t_server, Toast.LENGTH_SHORT).show();
                } else {
                    borraInst(context);
                    TreeMap<String, Long> paresInst = descodificarInst(datosEntrada);
                    TreeMap<Long, String> paresReg = descodificarReg(datosEntrada);
                    if (!paresInst.isEmpty()) {
                        Set<String> claves = paresInst.keySet();
                        for (String clave : claves) {
                            String nombre = clave;
                            //long fecha = paresInst.get(clave);
                            guardar(nombre);
                        }
                        creaNotif(context);
                        actualizaOcupados();
                    }
                    if (!paresReg.isEmpty()) {
                        Set<Long> claves = paresReg.keySet();
                        for (Long clave : claves) {
                            long fecha = clave;
                            String nombre = paresReg.get(clave);
                            guardarRegistro(nombre, fecha, context);
                        }
                    }
                }
            } else {
                Toast.makeText(context, R.string.t_server, Toast.LENGTH_SHORT).show();
            }
            http.disconnect();
        }
    }

    /**
     * Clase auxiliar para manejar la base de datos de las reservas.
     */
    private class ReservasOpener extends SQLiteOpenHelper{

        private static final String CREAR_TABLA = "CREATE TABLE " + RESERVAS_NAME +
                 " ( " + DATABASE_ID + " INT NOT NULL, " + DATABASE_RESERVA +
                " VARCHAR(255), PRIMARY KEY( " + DATABASE_ID + "))";

        ReservasOpener(Context c){
            super(c, RESERVAS_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREAR_TABLA);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP IF EXISTS TABLE " + RESERVAS_NAME);
            onCreate(db);
        }
    }

    /**
     * Clase auxiliar para manejar la base de datos de los avisos.
     */
    private class PeticionesOpener extends SQLiteOpenHelper{

        private static final String CREAR_TABLA = "CREATE TABLE " + PETICIONES_NAME +
                " ( " + DATABASE_ID + " INT NOT NULL, " + DATABASE_RESERVA +
                " VARCHAR(255), PRIMARY KEY( " + DATABASE_ID + "))";

        PeticionesOpener(Context c){
            super(c, PETICIONES_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREAR_TABLA);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP IF EXISTS TABLE " + PETICIONES_NAME);
            onCreate(db);
        }
    }

    /**
     * Clase auxiliar para manejar la base de datos de los registros para la historia pendientes.
     */
    private class HistoriaOpener extends SQLiteOpenHelper{

        private final String CREAR_TABLA = "CREATE TABLE " + HISTORIA_NAME +
                " ( " + DATABASE_ID + " INTEGER, " + DATABASE_RESERVA +
                " VARCHAR(255) NOT NULL, " + DATABASE_FECHA + " LONG NOT NULL, PRIMARY KEY( " + DATABASE_ID + "))";

        HistoriaOpener(Context c){
            super(c, HISTORIA_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREAR_TABLA);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP IF EXISTS TABLE " + HISTORIA_NAME);
            onCreate(db);
        }
    }

    /**
     * Clase encargada de gestionar la comunicación y añadir los instrumentos que estaban reservados
     * previamente junto a la creación de las notificaciones correspondientes.
     */
    private class Recuperar extends AsyncTask<String, Void, InputStream> {

        private Context context;
        private String user;
        private HttpURLConnection http;

        protected Recuperar(Context c){
            context = c;
        }

        /**
         * Inicia la comunicación con el servidor.
         * @param params Recibe como parámetro el nombre de usuario.
         * @return El stream recibido del servidor para su descodificación.
         */
        @Override
        protected InputStream doInBackground(String... params) {
            user = params[0];
            URL url;
            URLConnection urlC;
            InputStream in = null;
            OutputStream out;
            try {
                url = new URL(getDirBase() + "/instrumentos/recuperar.php");
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
                writer.write("usuario=" + pasarHexadecimal(user));
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
                String datosEntrada = leerDatos(stream);

                if (datosEntrada.equals("Null\n")) {
                } else if (datosEntrada.equals("Failed connection")) {
                } else {
                    TreeMap<String, Long> paresInst = descodificarInst(datosEntrada);
                    TreeMap<Long, String> paresReg = descodificarReg(datosEntrada);
                    if (!paresInst.isEmpty()) {
                        Set<String> claves = paresInst.keySet();
                        for (String clave : claves) {
                            String nombre = clave;
                            //long fecha = paresInst.get(clave);
                            guardar(nombre);
                        }
                        creaNotif(context);
                        actualizaOcupados();
                    }
                    if (!paresReg.isEmpty()) {
                        Set<Long> claves = paresReg.keySet();
                        for (Long clave : claves) {
                            long fecha = clave;
                            String nombre = paresReg.get(clave);
                            guardarRegistro(nombre, fecha, context);
                        }
                    }
                }
            } else {
            }
            http.disconnect();
            cuadro.dismiss();
        }
    }
}