<?php
	require_once '../nombres.php';
	$enlace = new mysqli($dbhost,$dbuser,$dbpsswd,$dbname);

	if($enlace->connect_errno){
		die("Failed connection");
	}

	$cabecera = " WHERE (";
	$seleccion = "";
	$nPedidos = 0;
	for($i = "1"; ; $i = (string) (1 + $i)){
		if($_POST["nombre" . $i] === null){
			break;
		}
		if($nPedidos !== 0){
			$seleccion = $seleccion . " OR ";
		}
		$seleccion = $seleccion . "nombre='" . $_POST["nombre" . $i] . "'";
		$nPedidos = $nPedidos + 1;
	}

	if($nPedidos === 0){
		$seleccion = "";
	}else{
		$seleccion = $cabecera . $seleccion . ") AND reserva=''";
	}
	if($entrada = $enlace->query("SELECT * FROM " . $dbreservas . $seleccion)){
		if($entrada->num_rows === 0){
			print("Null\n");
		}else{
			print("libres='");
			foreach($entrada as $fila){
				$j = "1";
				for( ; ; $j = (string) (1 + $j)){
					if($_POST["nombre" . $j] === null){
                		        	break;
		                	}elseif ($_POST["nombre" . $j] === $fila['nombre']){
						print($j . ", ");
						break;
					}
				}
			}
			print("'");
		}
		$entrada->close();
	}else{
		die("Error en la seleccion");
	}
	$enlace->close();
?>
