<!doctype html>
<html>
<head>
	<meta charset="utf-8">
	<title>Reserva de instrumento</title>
	<link rel='stylesheet' type='text/css' href='../normal.css' media='screen'>
	<link rel='stylesheet' type='text/css' href='../movil.css' media='(max-device-width:480px)'>
</head>
<body>
	<table id='centrar'><tr><td><header>
	<a href='../index.php'><img src='../logopequeno.png' width=25% height=25%></img></a></header>
	<?php
		require_once '../nombres.php';
		$enlace = new mysqli($dbhost, $dbuser, $dbpsswd, $dbname);

		if($enlace->connect_errno){
			die("Failed connection");
		}

		$no_query = 1;

		if($_REQUEST['save'] === 'yes')
			require_once 'saveWeb.php';

		if($no_query === 0){
			header("Location: ../index.php");
			die();
		} else {

	                echo "
	                <p>Incidencias de  " . $_POST['inst'] . "</p>";
	                if($entrada = $enlace->query("SELECT * FROM " . $dbhistorial . " WHERE reserva='" . $_POST['inst'] . "' ORDER BY fecha DESC")){
	                        if($entrada->num_rows !== 0){
	                                foreach($entrada as $fila){
						$out = htmlentities($fila['incidencias'], ENT-SUBSTITUTE, 'ISO-8859-1');
	                                        echo '
	                                        <p><b>Incidencias anteriores</b></p>
	                                        <p>';
						echo nl2br($out);
						echo '</p>';
	                                        break;
	                                }
	                        }
	                        $entrada->close();
	                } else {
	                        die("Error al realizar la búsqueda");
	                }

			echo "
			<p>Reservar " . $_POST['inst'] . "</p>
			<form action='reservaWeb.php' method='post'>
			<fieldset>
			<input type='hidden' name='inst' value='" . $_POST['inst'] . "' />
			<input type='hidden' name='save' value='yes' />
			<p><label>Usuario</label><br />
			<input type='text' name='user' /></p>
			<p><label>Contraseña</label><br />
			<input type='password' name='cont' /></p>
			<br />
			<p><textarea cols='40' rows='5' placeholder='Incidencias' name='incidencias'></textarea></p>
			<input type='radio' name='_tipo' value='cuatro' checked='checked' />Sin cambios<br />
                        <input type='radio' name='_tipo' value='uno' />Incidencias solucionadas<br />
                        <input type='radio' name='_tipo' value='dos' />Añadir incidencias<br />
			<input type='radio' name='_tipo' value='tres' />Incidencias solucionadas y nuevas añadidas<br />
			<button type='submit'>Aceptar</button>
			</fieldset></form>";
		}


		$enlace->close();
	?>
	</td></tr></table>
</body>
</html>
