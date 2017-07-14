<!doctype html>
<html>
<head>
	<meta charset="utf-8">
	<title>Administrar usuarios</title>
	<link rel='stylesheet' type='text/css' href='../normal.css' media='screen'>
	<link rel='stylesheet' type='text/css' href='../movil.css' media='(max-device-width:480px)'>
</head>
<body>
	<table id='centrar'><tr><td><header>
	<a href='../index.php'><img src='../logopequeno.png' width=25% height=25%></img></a></header>
	<p>Administrar usuarios:</p>
	<?php
		session_start();
		require_once '../nombres.php';
		$enlace = new mysqli($dbhost, $dbuser, $dbpsswd, $dbname);

		if($enlace->connect_errno){
			die("Failed connection");
		}

		$no_query = 1;

		if($_REQUEST['admin'] === 'yes')
			require_once 'admin.php';

		if($_SESSION['inicio'] === '1' || $no_query === 0){
			$_SESSION['inicio'] = '1';
                        echo "<p><a href='anadir.php'>Añadir usuario</a></p>";
                        echo "<p><a href='borrar.php'>Borrar usuario</a></p>";
		} else {
			if($entrada = $enlace->query("SELECT permisos FROM " . $dbusuarios . " WHERE permisos='1'")){
				if($entrada->num_rows === 0){
					$_SESSION['inicio'] = '1';
					echo "<p><a href='anadir.php'>Añadir usuario</a></p>";
				}else{
					echo "
					<form action='administrar.php' method='post'>
					<fieldset>
					<input type='hidden' name='admin' value='yes' />
					<p><label>Usuario</label><br />
					<input type='text' name='user' /></p>
					<p><label>Contraseña</label><br />
					<input type='password' name='cont' /></p>
					<button type='submit'>Aceptar</button>
					</fieldset></form>";
				}
				$entrada->close();
			}else{
				die("Error al realizar la búsqueda");
			}
		}
		$enlace->close();
	?>
	</td></tr></table>
</body>
</html>
