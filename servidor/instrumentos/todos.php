<?php
	require_once '../nombres.php';
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
		if($entrada->num_rows === 0){
			print("Null\n");
		}else{
			foreach($entrada as $fila){
				print("nombre='" . $fila['nombre'] . "'&reserva='" . $fila['reserva'] . "'&fecha='" . $fila['fecha'] . "'\n");
			}
		}
		$entrada->close();
	}else{
		die("Error en la seleccion");
	}
	$enlace->close();
?>
