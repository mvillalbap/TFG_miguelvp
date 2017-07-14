<!doctype html>
<html>

<head>
        <title>Administración de reserva de instrumentos</title>
        <meta charset='utf-8'>
        <link rel='stylesheet' type='text/css' href='normal.css' media='screen'>
        <link rel='stylesheet' type='text/css' href='movil.css' media='(max-device-width:480px)'>
</head>

<body>
        <table id='centrar'><tr><td>
		<header>
			<a href='index.php'><img src='logopequeno.png' width=25% height=25%> </img></a>
                        <p><form action='index.php' method='get'>
                                <input type='text' placeholder='Buscar' name='_query' />
                                <button type='submit'>&#x1f50d;</button> <br />
                                <input type='radio' name='_tipo' value='instrumento' checked='checked' />Instrumento
                                <input type='radio' name='_tipo' value='usuario'/>Usuario
                        </form></p>
                </header>

                <div id='main'>
                        <table id='tabla'>
                                <?php
					session_start();
					require_once 'nombres.php';
                                        $enlace = new mysqli($dbhost,$dbuser,$dbpsswd,$dbname);

                                        if($enlace->connect_errno){
                                                die("Failed connection");
                                        }

                                        $seleccion = "";
					$caracNoAdmit = array("'", "\\");
                                        if(!($_GET['_query'] === null || $_GET['_query'] === '')){
						$_GET['_query'] = str_replace($caracNoAdmit, "", $_GET['_query']);
                                                if($_GET['_tipo'] === 'usuario')
                                                        $seleccion = "WHERE reserva LIKE '%" . $_GET['_query'] . "%'";
                                                else
                                                        $seleccion = "WHERE nombre LIKE '%" . $_GET['_query'] . "%'";
                                        }
                                        if($entrada = $enlace->query("SELECT * FROM " . $dbreservas . " " . $seleccion . " ORDER BY reserva ASC, nombre ASC")){
                                                echo "<tr><th>Instrumento</th><th>Reservado</th><th>Desde</th></tr>";
                                                foreach($entrada as $fila){
                                                        $fecha = "";
                                                        if($fila['fecha'] === '0'){
                                                                $fecha = "";
                                                        }else{
                                                                date_default_timezone_set("Europe/Madrid");
                                                                $fecha = date("d/m (H:i)", $fila['fecha']);
                                                        }
                                                        echo "<tr><td>
								<form action='session/reservaWeb.php' method='post'>
								<button type='submit' name='inst' value='" . $fila['nombre'] . "' class='reserva-link'>" . $fila['nombre'] . "</button>
								</form></td>
								<td>" . $fila['reserva'] . "</td>
								<td>" . $fecha . "</td></tr>";
                                                }
                                                $entrada->close();
                                        }else{
                                                die("Error en la seleccion");
                                        }
                                        $enlace->close();
                                ?>
                        </table>
                </div>
        </td></tr>
        <tr><td><footer>
                <div><a href='session/administrar.php'>Administrar usuarios</a></div>
                <div><a href='session/cambiar.php'>Cambiar contrasena</a></div>
        </footer></td></tr></table>

</body>

</html>
