<!doctype html>
<html>
<head>
	<meta charset="utf-8">
	<title>Cambiar contraseña</title>
	<link rel='stylesheet' type='text/css' href='../normal.css' media='screen'>
</head>
<body>
	<table id='centrar'><tr><td><header>
	<a href='../index.php'><img src='../logopequeno.png' height=25% width=25%></img></a></header>
	<p>Cambiar contraseña:</p>
	<?php
		require_once '../nombres.php';

		$enlace = new mysqli($dbhost, $dbuser, $dbpsswd, $dbname);

		if($enlace->connect_errno){
			die("Failed connection");
		}

		if($_REQUEST['change'] === 'yes')
			require_once 'change.php';

		echo "
		<form action='cambiar.php' method='post'>
		<fieldset>
		<input type='hidden' name='change' value='yes' />
		<p><label>Usuario</label><br />
		<input type='text' name='usuario' /></p>
		<p><label>Contraseña antigua</label><br />
		<input type='password' name='contAnt' /></p>
		<p><label>Contraseña nueva</label><br />
		<input type='password' name='cont' /></p>
		<p><label>Repetir contraseña</label><br />
		<input type='password' name='repet' /></p>
		<button type='submit'>Cambiar</button>
		</fieldset></form>";
		$enlace->close();
	?>
	</td></tr></table>
</body>
</html>
