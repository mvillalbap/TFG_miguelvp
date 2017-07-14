<?php
	if($entrada = $enlace->query("SELECT * FROM " . $dbusuarios . " WHERE usuario='" . $_POST['usuario'] . "'")){
		if($entrada->num_rows === 0)
			die("Failed connection");
	}else{
		die("Error al realizar la búsqueda");
	}
	$entrada->close();
?>
