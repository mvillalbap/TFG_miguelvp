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
	<title>Borrar usuario</title>
	<link rel='stylesheet' type='text/css' href='../normal.css' media='screen'>
</head>
<body>
	<table id='centrar'><tr><td><header>
	<a href='../index.php'><img src='../logopequeno.png' height=25% width=25%></img></a></header>
	<p>Borrar usuario:</p>
	<?php
		require_once '../nombres.php';
		$enlace = new mysqli($dbhost, $dbuser, $dbpsswd, $dbname);

		if($enlace->connect_errno){
			die("Failed connection");
		}

		if($_REQUEST['erase'] === 'yes')
			require_once 'erase.php';

		echo "
		<form action='borrar.php' method='post'>
		<fieldset>
		<input type='hidden' name='erase' value='yes' />
		<p>El usuario:</p>
		<p><label>Usuario</label><br />
		<input type='text' name='admin' /></p>
		<p><label>Contraseña</label><br />
		<input type='password' name='adcont' /></p>
		<p>elimina de la base de datos al usuario:</p>
		<p><label>Usuario</label><br />
		<input type='text' name='usuario' /></p>
		<button type='submit'>Borrar</button>
		</fieldset></form>";
		$enlace->close();
	?>
	</td></tr></table>
</body>
</html>
