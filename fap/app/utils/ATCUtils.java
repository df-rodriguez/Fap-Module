package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.util.regex.*;

import org.h2.constant.SysProperties;
import org.joda.time.DateTime;

import com.lowagie.text.pdf.codec.Base64.InputStream;

import config.InjectorConfig;

import properties.FapProperties;
import enumerado.fap.gen.CodigoRespuestaEnum;
import enumerado.fap.gen.EstadosPeticionEnum;
import enumerado.fap.gen.ListaEstadosEnum;
import enumerado.fap.gen.ListaOrigenEnum;

import reports.Report;
import services.GestorDocumentalService;
import services.GestorDocumentalServiceException;
import validation.CifCheck;

import messages.Messages;
import models.AEAT;
import models.ATC;
import models.Cesiones;
import models.Documento;
import models.PeticionCesiones;
import models.SolicitudGenerica;

public class ATCUtils {

	static final int nombreCte = 75;
	static final int libreCte = 72;
	static final String cambioPagina = "----------";
	private static final String NIF_NIE_ASOCIATION = "TRWAGMYFPDXBNJZSQVHLCKET";
	
	//Inyeccion manual	
	static GestorDocumentalService gestorDocumentalService = InjectorConfig.getInjector().getInstance(GestorDocumentalService.class);
	
	public static void peticionATC(PeticionCesiones pt, List<Long> idsSeleccionados){
		File file = null;
		BufferedWriter bw = null;
		try{
			file = File.createTempFile("ATC"+obtenerFechaFormato(), ".txt");
			bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
		}
		catch (IOException e) {
			Messages.error("Error generando el fichero de peticion, intentelo de nuevo");
		}
		//Contenido = RegCabecera (tipoReg + fecha + numSol + libre) + RegDetalle (tipoReg + NDoc + Nombre)
		Formatter fmt = new Formatter();
		fmt.format("%04d", idsSeleccionados.size());
		String numSol = ""+fmt;
		String libre = String.format("%"+libreCte+"s", "");
		String contenido ="0"+fechaFormato()+numSol+libre+"\n";
		Set idNoRepetidos = new HashSet(); 
		
		//No puede haber dni/cif repetidos
		for (Long solId : idsSeleccionados) {
			SolicitudGenerica s = SolicitudGenerica.findById(solId);
				if ((s.solicitante.isPersonaFisica()) && (!idNoRepetidos.contains(s.solicitante.fisica.nip.valor))){
					contenido += "1";
					String nombre = s.solicitante.nombreCompleto;
					String nombre75 = nombre + String.format("%"+(nombreCte-nombre.length())+"s", "")+"\n";
					contenido += s.solicitante.fisica.nip.valor+nombre75.substring(0, nombreCte);				
					idNoRepetidos.add(s.solicitante.fisica.nip.valor);
				}
				if ((s.solicitante.isPersonaJuridica()) && (!idNoRepetidos.contains(s.solicitante.juridica.cif))){
					contenido += "1";
					String nombre = s.solicitante.nombreCompleto;
					String nombre75 = nombre + String.format("%"+(nombreCte-nombre.length())+"s", "")+"\n";
					contenido += s.solicitante.juridica.cif+nombre75.substring(0, nombreCte);
					idNoRepetidos.add(s.solicitante.juridica.cif);
				}
		}
		try {
			bw.write(contenido);
			bw.close();
			Documento doc = new Documento();
        	doc.tipo = FapProperties.get("fap.aed.tiposdocumentos.peticionATC");
        	doc.descripcion = "Descripcion Peticion ATC";
        	gestorDocumentalService.saveDocumentoTemporal(doc, new FileInputStream(file), "ATC"+obtenerFechaNombre()+".txt");
        	pt.fichPeticion.tipo = FapProperties.get("fap.aed.tiposdocumentos.peticionATC");
        	pt.fichPeticion.uri =  doc.uri; //Almaceno donde está ANTES getAbsolutepath
			pt.estado = EstadosPeticionEnum.creada.name();
			pt.fechaGen=DateTime.now();
			pt.fechaValidez = pt.fechaGen.plusMonths(Integer.parseInt(FapProperties.get("fap.cesiondatos.validezPeticion")));
			pt.save();
			pt.fichPeticion.save();
		} catch (IOException e) {
			Messages.error("Error escribiendo en el fichero de peticion, intentelo de nuevo");
		} catch (GestorDocumentalServiceException e) {
			Messages.error("Error subiendo el fichero de petición al AED");
		}
	
	}
	
	private static String obtenerFechaFormato() {
		Calendar fecha =  Calendar.getInstance();
		int dia = fecha.get(Calendar.DATE);
		int mes = fecha.get(Calendar.MONTH);
		int anyo = fecha.get(Calendar.YEAR);
		Formatter fmtMes = new Formatter();
		fmtMes.format("%02d", mes);
		Formatter fmtDia = new Formatter();
		fmtDia.format("%02d", dia);
		return fmtDia.toString()+fmtMes.toString()+anyo;
	}
	
	private static String fechaFormato() {
		Calendar fecha =  Calendar.getInstance();
		int dia = fecha.get(Calendar.DATE);
		int mes = fecha.get(Calendar.MONTH);
		int anyo = fecha.get(Calendar.YEAR);
		Formatter fmtMes = new Formatter();
		fmtMes.format("%02d", mes);
		Formatter fmtDia = new Formatter();
		fmtDia.format("%02d", dia);
		return anyo+fmtMes.toString()+fmtDia.toString();
	}

	private static String obtenerFechaNombre() {
		Calendar fecha =  Calendar.getInstance();
		int dia = fecha.get(Calendar.DATE);
		int mes = fecha.get(Calendar.MONTH);
		int anyo = fecha.get(Calendar.YEAR);
		int hora = fecha.get(Calendar.HOUR);
		int min = fecha.get(Calendar.MINUTE);
		Formatter fmtMes = new Formatter();
		fmtMes.format("%02d", mes);
		Formatter fmtDia = new Formatter();
		fmtDia.format("%02d", dia);
		return fmtDia.toString()+fmtMes.toString()+anyo+hora+min;
	}
	
	public static void parsearATC(PeticionCesiones pt, File fich) {
		try {
			String contenido = FileUtils.ConvertPDFToString(fich.toString());
			Boolean datos = false;
			String[] tokens = contenido.split("\n");
			Pattern pattern = Pattern.compile("([0-9]{2}/[0-9]{2}/[0-9]{4})");
			Matcher matcher = null;
			
			for (int i = 0; i<tokens.length;i++) {
				matcher = pattern.matcher(tokens[i]); //Fecha de generacion
				while (matcher.find())
					pt.respCesion.fechaGeneracion = obtenerFecha(tokens[i]);
				if (tokens[i].equals("DESCRIPCION RESPUESTA")){
					datos = true;
				}
				if (tokens[i].equals(cambioPagina)){
					datos = false;
				}
				if ((datos) && (!tokens[i].equals("DESCRIPCION RESPUESTA"))){
					ATC atc = new ATC();
					atc.registroDetalle.nDocumento = tokens[i];
					atc.nombre = tokens[++i];
					atc.codigoEstado = tokens[++i];
					atc.registroDetalle.estado = tokens[++i];
					generarPdfATC(pt, atc);
				}
			}
		} catch (Exception e) {
			Messages.error("Error parseando el documento de respuesta del ATC, compruebe que el fichero es correcto");
		}
	}

	private static File generarPdfATC(PeticionCesiones pt, ATC atc){
		File report =  new File ("reports/bodyPeticion.html");
		String tipo = getTipo(atc.registroDetalle.nDocumento);
        //Obtener solicitud correspondiente
        //Tipo dice si es dni, nie, pasaporte, cif
        List<SolicitudGenerica> solicitud = null;
        if (tipo.equals("NIE")){ //dni, pasaporte,..
        	solicitud = SolicitudGenerica.find("Select solicitud from Solicitud solicitud where solicitud.solicitante.fisica.nip.valor = ?", atc.registroDetalle.nDocumento).fetch();
        }
        else if (tipo.equals("CIF")){
        	solicitud = SolicitudGenerica.find("Select solicitud from Solicitud solicitud where solicitud.solicitante.juridica.cif = ?", atc.registroDetalle.nDocumento).fetch();
        }
		
        if((!Messages.hasErrors()) && (!solicitud.isEmpty())){
        	try {
            	play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer.addVariable("pt", pt);
            	for (SolicitudGenerica sol : solicitud) {
            		play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer.addVariable("atc", atc);
            		play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer.addVariable("sol", sol);
                	report = new Report("reports/bodyPeticionATC.html").header("reports/headerPeticion.html").footer("reports/footer-cesion.html").renderTmpFile(sol, pt, atc);
                	Documento doc = new Documento();
                	doc.tipo = FapProperties.get("fap.aed.tiposdocumentos.respuestaAEAT");
                	doc.descripcion = "Descripcion ATC";
                	gestorDocumentalService.saveDocumentoTemporal(doc, new FileInputStream(report), "cesionATC"+obtenerFechaNombre()+".pdf");
                	sol.documentacionCesion.documentos.add(doc);
                	pt.respCesion.fechaActuacionGestor = new DateTime();
                	pt.respCesion.uri = doc.uri;
                	aplicarCambios(sol, pt, doc, atc.codigoEstado);
            	}
            } catch (Exception ex2) {
                Messages.error("Error generando el documento pdf: "+ex2.getMessage());
                play.Logger.error("Error generando el documento pdf: "+ex2.getMessage());
            }
        }
        else{
          	Messages.info("La cesion de datos para "+atc.registroDetalle.nDocumento+", no se corresponde con ninguna solicitud");
        }
		
		return null; //Provisional
	}
	
	private static void aplicarCambios(SolicitudGenerica solicitud, PeticionCesiones pt, Documento doc, String estado) {
		Cesiones cesion = new Cesiones();
		cesion.tipo = pt.tipo;
		cesion.fechaPeticion = pt.respCesion.fechaGeneracion;
		cesion.fechaValidez = pt.fechaValidez;
		cesion.origen = ListaOrigenEnum.cesion.name();
		cesion.documento = doc;
		cesion.estado = CodigoRespuestaEnum.valueOf("_"+estado).name();	
		solicitud.cesion.cesiones.add(cesion);
		solicitud.save(); //Guardar cambios en la solicitud
	}

	private static String getTipo (String numdoc){
		StringBuilder texto = new StringBuilder();
		if (CifCheck.validaCif(numdoc, texto)) //Si es un cif
			return "CIF";
		else if (checkNifNieLetter(numdoc)){ //Si es dni la letra tiene que ser correcta
			return "NIE";
		}
		else
			return "ERROR";	
	}
	
	private static boolean checkNifNieLetter(String numero){
		int digitosNif = Integer.parseInt(numero.substring(0,8));
		int letraEsperada = NIF_NIE_ASOCIATION.charAt(digitosNif % 23); 
		int letraActual = numero.charAt(8);
		return (letraEsperada ==  letraActual);
	}
	
	public static DateTime obtenerFecha(String fecha){
		int dia = Integer.parseInt(fecha.substring(0, 2));
		int mes = Integer.parseInt(fecha.substring(3, 5));
		int anio = Integer.parseInt(fecha.substring(6, fecha.length()));
		
		DateTime fechaGeneracion = new DateTime(anio, mes, dia, 0, 0);
		return fechaGeneracion;
	}
}