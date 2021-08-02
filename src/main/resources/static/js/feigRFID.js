/*
 * 
 */
const Actions = {
	ANTENNA : 0,
	TIME : 1,
	VALIDTIME : 2,
	POWER : 3,
	RESET_READER_FILE : 4,
	TOGGLE_MODE : 5,
	TOGGLE_RELAIS : 6,
	ANTENNA_CHECKBOXES :7,
	INFO: 8,
}

var t;
// Some kind of flag that ensures no two xhr requests happen at the same time?
var isRunning = false;

function handleTabButtons(r) {
	clearTimeout(t);
	if($( '#reader-' + r + '-live').css('display') == 'block') {
		handleReaderResults(r , 'Aktuell_' + r + '.out', 'showlive'); 
		t = setTimeout(function() { 
			handleTabButtons(r);
			}, 5000);
	}
}

function handleWriteButtons(r, mode) {
	var error = true;
	var readerIp = r.replace(/_/g, '.');
	
	$('#faultstring-write-' + r).html('no reader connected');
	$('#faultstring-write-' + r).css("display","none");

	//$('#successstring-write-' + r).css("display","none");

	if( $('#mode-' + r).val() == 'BRM' ) {
		$('#faultstring-write-' + r).html('Enable ISO Mode');
		$('#faultstring-write-' + r).css("display","block");
		return;
	}
	
	if(!parseInt( $('#newnr-'+r).val() )) {
		$('#faultstring-write-' + r).html( 'Die Startnummer muss eine Zahl sein' );
		$('#faultstring-write-' + r).css("display","block");
		return;
	}
	
	if( mode == 0 ) {
		$('#stopWriteTag-' + r).css("display","inline-block");
		$('#writeTag-' + r).css("display","none");
	}
	
	if(!isRunning) {
		isRunning = true;
		
		var jqxhr = $.getJSON( '/api/' + readerIp + '/write/' + $('#newnr-'+r).val() );
		jqxhr.done(function( data ) {
			if(data != "") {
				$.each( data, function( key, val ) {
					if(key == 'success' && val == 'true') { 
						error = false;
					}
					
					if(key == 'message' && val != '') {
						$('#faultstring-write-' + r).html( val );
					}
				});
				
				if(!error) {
					if(mode == 0) {
						$('#newnr-' + r).val( parseInt(data['stnr'])+1 );
					}
					$('#successstring-write-' + r).html('Startnummer: ' + data['stnr'] + ' - Seriennummer: ' + data['newSerialNumber']);
					$('#successstring-write-' + r).css("display","block");
				}
				
				if(error) {
					$('#faultstring-write-' + r).css("display","block");
				}	
				
			}
			isRunning = false;
		});
	}

	if(mode == 0) {
		t = setTimeout(function() { 
			handleWriteButtons(r, 0);
			}, $('#ConfigWriteSleepTime').val() * 1000);
	}

}

function handleStopWrite(r) {
	clearTimeout(t);
	$('#stopWriteTag-' + r).css("display","none");
	$('#writeTag-' + r).css("display","inline-block");
}


/**
 * Actually does something more like setReaderData. This function is used to set various reader related properties such
 * as power and antenna state
 * @param {string} r the reader name as fed in from the readers application.properties with dots replaced as underscores
 * @param {Actions} a the action to be done
 */
function getReaderData(r, a) {
	var action = 'info';
	var readerIp = r.replace(/_/g, '.');
	// Generate the api endpoint route
	switch(a) {
		case Actions.ANTENNA:
			action = 'ant/' + $('#antenna-'+r).val();
			break;
		case Actions.TIME:
			action = 'time';
			break;
		case Actions.VALIDTIME:
			action = 'validtime/' + $('#transponderValidTime-'+r).val();
			break;
		case Actions.POWER:
			action = 'power/' + $('#power-'+r).val();
			break;
		case Actions.RESET_READER_FILE:
			action = 'resetReaderFile';
			break;
		case Actions.TOGGLE_MODE:
			// Toggle between BRM and ISO modes
			var newmode = 'BRM';
			if($('#mode-'+r).val() == 'BRM') { newmode = 'ISO'; }
			action = 'mode/' + newmode;
			break;
		case Actions.TOGGLE_RELAIS:
			// Toggle between on and off
			var newval = 'on';
			if($('#relais-'+r).val() == 'on') { newval = 'off'; }
			action = 'relais/' + newval;
			break;
		case Actions.ANTENNA_CHECKBOXES:
			let asString = parseCheckboxIntoBitsetString(r);
			action = 'ant/' + asString;
	}
	
	$('#faultstring-' + r).css("display","none");
	
	
	if(action != 'info') { 
		isRunning = true;
		var jqxhr = $.getJSON( '/api/' + readerIp + '/' + action);
		// After action done, update website
		jqxhr.done(function( _data ) {
			getReaderInfo(r);
			isRunning = false;
		});
	} else {
		getReaderInfo(r);
	}

}

/**
 * @param r the reader name just like in getReaderData
 * @returns {string} The bitset string representation of the check boxes for reader state
 */
function parseCheckboxIntoBitsetString(r) {
	let a1 = $('#antenna1-'+r)[0].checked ? "1" : "0";
	let a2 = $('#antenna2-'+r)[0].checked ? "1" : "0";
	let a3 = $('#antenna3-'+r)[0].checked ? "1" : "0";
	let a4 = $('#antenna4-'+r)[0].checked ? "1" : "0";

	return a4+a3+a2+a1
}

/**
 * Updates the config boxes and reader output boxes of reader r
 * Calls de.opentiming.feigWS.reader.ReaderInfo api
 * @param r
 */
function getReaderInfo(r) {
	
	var readerIp = r.replace(/_/g, '.');
	var error = true;
	
	var jqxhr = $.getJSON( '/api/' + readerIp + '/info');
	jqxhr.done(function( data ) {
		if(data != "") {
			$.each( data, function( key, val ) {
				if(key == "mode") { 
					error = false;
				}
				
				if(key == "files") {
					setTableData(r, val, readerIp);
				}
				if (key == "antenna"){
					setAntennaCheckboxes(r, val);
				}
				
				$('#' + key + '-' + r).val(val);
			});
		}

		if(error) {
			$('#faultstring-' + r).html('no reader connection');
			$('#faultstring-' + r).css("display","block");
		}
	});
}

/**
 * Updates the checkboxes or reader r
 * @param r
 * @param antenna
 */
function setAntennaCheckboxes(r, antenna){
	$('#antenna1-'+r)[0].checked = antenna[3] === "1";
	$('#antenna2-'+r)[0].checked = antenna[2] === "1";
	$('#antenna3-'+r)[0].checked = antenna[1] === "1";
	$('#antenna4-'+r)[0].checked = antenna[0] === "1";
}

function setTableData(r, val, readerIp) {
	table = "";

	val.sort(function(a, b) {
	    return a.file > b.file;
	});

	val.reverse(); 

	$.each(val, function(key, val) {
	    table = table + "<tr><td>" + val['file'] + "</td>" +
		"<td class='text-right'>" + val['linecount'] + "</td>" + 
    	"<td><a class='btn btn-success btn-sm' href='#' onclick=\"handleReaderResults('" + readerIp + "','" + val['file'] + "', 'show'); return false;\" role='button'>SHOW</a></td>" + 
		"<td><a class='btn btn-success btn-sm' href='/api/download/" + val['file'] + "' download='" + val['file'] + "' role='button'>DOWNLOAD</a></td>" +
		"</tr>";
	});
	
	$('#tbody-'+r).html(table);
}


function handleReaderResults(r, file, mode) {
	if(mode == "show") {
		target = "#modal-body";
		$( '#modal' ).modal();
	}
	
	if(mode == "showlive") {
		target = "#showlive-" + r;
	}

	if(!isRunning) {
		isRunning = true;
		
		var jqxhr = $.getJSON( "/api/" + r + "/file/" + file);
	
		//$( target ).html( '' );
		jqxhr.done(function( data ) {
			$( target ).html( showReaderResults(data) );
			isRunning = false;
		});
		
	}
}

function showReaderResults(data) {

	var html = '';
	html = html + '<div class="table-responsive">' +
		'<table class="table table-striped table-vcenter">' +
		'<thead>' +
			'<tr>' +
				'<th>Startnummer</th>' +
				'<th>Datum</th>' +
				'<th>Uhrzeit</th>' +
				'<th>Milli</th>' +
				'<th>Reader</th>' +
				'<th>Antenne</th>' +
				'<th>RSSI</th>' +
				'<th>UID</th>' +
				'<th>Lesezeit</th>' +
			'</tr>' +
		'</thead>' +
		'<tbody>';
	

	data.reverse(); 
	
	var max = 100
	var count = 0;
	$.each(data, function(key, val) {
		var fields = val.split(";");
		html = html + '<tr>';
		$.each(fields, function(key, val) {
			html = html + '<td>' + val + '</td>';
		});
		html = html + '</tr>';
		count++;
		if(count > max) { return false; }
	});

		'</tbody>' +
	'</table>' +
	'</div>';
	
	return html;
}


function clearModal() {
	var data = '<span class="text-muted">loading...</span>';
	$( '#modal-body' ).html( data );
}