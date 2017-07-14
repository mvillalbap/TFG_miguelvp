<?php
	session_start();
	if($_SESSION['inicio'] === NULL){
		header("Location: administrar.php");
		die();
	}
?>
<!doctype html>
<html>
<head>
	<meta charset="utf-8">
	<title>Añadir usuario</title>
	<link rel='stylesheet' type='text/css' href='../normal.css' media='screen'>
</head>
<body>
	<table id='centrar'><tr><td><header>
	<a href='../index.php'><img src='../logopequeno.png' width=25% height=25%></img></a></header>
	<p>Añadir usuario:</p>
	<?php

		require_once '../nombres.php';
		$enlace = new mysqli($dbhost, $dbuser, $dbpsswd, $dbname);

		if($enlace->connect_errno){
			die("Failed connection");
		}

		if($_REQUEST['add'] === 'yes')
			require_once 'add.php';

		if($entrada = $enlace->query("SELECT permisos FROM " . $dbusuarios . " WHERE permisos='1'")){
			echo "
			<form action='anadir.php' method='post'>
			<fieldset>
			<input type='hidden' name='add' value='yes' />";
			if($entrada->num_rows === 0){
				echo "<p>El usuario nuevo tendrá los permisos de administración:</p>
				<p><label>Usuario</label><br />
				<input type='text' name='usuario' /></p>
				<p><label>Contraseña</label><br />
				<input type='password' name='cont' /></p>
				<p><label>Repite la contraseña</label><br />
				<input type='password' name='repet' /></p>
				<input type='hidden' name='permisos' value='Si' />
				<input type='hidden' name='empty' value='yes' />
				<button type='submit'>Añadir</button>";
			}else{
				echo "<p>El usuario:</p>
				<p><label>Usuario</label><br />
				<input type='text' name='admin' /></p>
				<p><label>Contraseña</label><br />
				<input type='password' name='adcont' /></p>
				<p>Da permiso para que se añada a la base de datos el usuario:</p>
				<p><label>Introduce un nombre de usuario</label><br />
				<input type='text' name='usuario' /></p>
				<p><label>Introduce una contraseña</label><br />
				<input type='password' name='cont' /></p>
				<p><label>Introduce de nuevo la contraseña</label><br />
				<input type='password' name='repet' /></p>
				<p><input type='checkbox' name='permisos' value='Si' />Tiene permisos</p>
				<button type='submit'>Añadir</button>";
			}
			echo "</fieldset></form>";
			$entrada->close();
		}else{
			die("Error al realizar la búsqueda");
		}
		$enlace->close();
	?>
	</td></tr></table>
</body>
</html>
