<?php
	require_once '../nombres.php';
	require_once '../conectar.php';
	require_once 'usuario.php';

	try{
		$enlace->query("DELETE FROM " . $dbreservas . " WHERE nombre='" . $_POST['nombre'] . "'");
		print("OK\n");
	} catch(Exception $error){
		print("False\n");
		die("Error al insertar: " . $error->getMessage());
	}
	$enlace->close();
?>
