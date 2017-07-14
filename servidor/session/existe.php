<?php
	require_once '../nombres.php';
	require_once '../conectar.php';

	$usercnt = '';
	$desp = X;
	$rellacumulado = 0;
	$clavePrivada = X;
	$claveModulo = XX;

	for($i = 0; $temp = substr($_POST['cont'],$i,4); $i = $i + 4 + (4 - $desp)){
		$sol = 1;
		$cte = intval($temp);
		for($k = 0; $k < 4; $k++){
			$rellacumulado = $rellacumulado + (floor($cte / pow(10,3-$k)) % 10);
		}
		$rellacumulado = ($rellacumulado * $rellacumulado) % 9507;
		for($desp = 0; $desp < 4; $desp++){
			$cero = floor($rellacumulado / pow(10, 3-$desp)) % 10;
			if($cero !== 0){
				break;
			}
		}
		for($j = 1; $j <= $clavePrivada; $j++){
			$sol = ($sol * $cte) % $claveModulo;
		}
		$usercnt .= chr($sol);
	}

	if($entrada = $enlace->query("SELECT * FROM " . $dbusuarios . " WHERE usuario='" . $_REQUEST['usuario'] . "'")){
		if($entrada->num_rows === 0){
			print("False");
		}else{
			$comparar = hash("sha256", $usercnt);
			foreach($entrada as $fila){
				if($fila['contrasena'] === $comparar){
					if($fila['permisos'] === '1'){
						print("Sudo\n");
					}else{
						print("OK\n");
					}
				}else{
					print("False\n");
				}
			}
		}
		$entrada->close();
	} else {
		die("Error al realizar la búsqueda");
	}

	$enlace->close();
?>
