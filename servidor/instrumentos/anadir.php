<?php
	require_once '../nombres.php';
	require_once '../conectar.php';
	require_once 'usuario.php';

	$duplicado = 0;
	if($entrada = $enlace->query("SELECT * FROM " . $dbusuarios . " WHERE usuario='" . $_POST['usuario'] . "'")){
		foreach($entrada as $fila){
			if($fila['permisos'] === '1'){
				$duplicado = 1;
			}
		}
		$entrada->close();
	}else{
		die("Error al realizar la búsqueda");
	}
	if($entrada = $enlace->query("SELECT * FROM " . $dbreservas . " WHERE nombre='" . $_POST['nombre'] . "'")){
		if($entrada->num_rows === 0){
			$entrada->close();
			try{
				$enlace->query("INSERT INTO " . $dbreservas . " values ('" . $_POST['nombre'] . "', '', '0')");
				print("OK\n");
			} catch(Exception $error){
				die("Error al insertar: " . $error->getMessage());
			}
		}else{
			if($duplicado === 1){
				print("Duplicado\n");
			}else{
				print("False\n");
			}
		}
		$entrada->close();
	} else {
		die("Error al realizar la búsqueda");
	}

	$enlace->close();
?>
