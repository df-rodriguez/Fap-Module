package services.filesystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import controllers.fap.FirmaController;

import es.gobcan.platino.servicios.sfst.SignatureServiceException_Exception;

import net.java.dev.jaxb.array.StringArray;

import messages.Messages;
import models.Documento;
import models.Firmante;

import platino.Firma;
import platino.InfoCert;
import play.libs.Codec;
import play.libs.Crypto;
import play.modules.guice.InjectSupport;
import properties.FapProperties;
import services.FirmaService;
import services.FirmaServiceException;
import services.GestorDocumentalService;
import services.platino.PlatinoFirmaServiceImpl;
import utils.BinaryResponse;

@InjectSupport
public class FileSystemFirmaServiceImpl implements FirmaService {
	
	private static Logger log = Logger.getLogger(FileSystemFirmaServiceImpl.class);
	
	@Inject
    protected static GestorDocumentalService gestorDocumentalService;
	
    @Override
    public boolean isConfigured() {
        //No necesita configuración
        return true;
    }
    
    @Override
    public void mostrarInfoInyeccion() {
		if (isConfigured())
			play.Logger.info("El servicio de Firma ha sido inyectado con FileSystem y está operativo.");
		else
			play.Logger.info("El servicio de Firma ha sido inyectado con FileSystem y NO está operativo.");
    }

    @Override
    public List<String> getFirmaEnClienteJS() {
        List<String> jsclient = new ArrayList<String>();
        jsclient.add("/public/javascripts/firma/base64.js");
        jsclient.add("/public/javascripts/firma/firma.js");
        jsclient.add("/public/javascripts/firma/firma-fs.js");
        return jsclient;
    }
    
    @Override
    public String firmarTexto(byte[] texto) throws FirmaServiceException {
        FileSystemFirma fsFirma = FileSystemFirma.encode("APP", "APPNIF", new String(texto));
        return fsFirma.encode();
    }

    @Override
    public boolean validarFirmaTexto(byte[] texto, String firma) throws FirmaServiceException {
        FileSystemFirma decodeFirma = FileSystemFirma.decode(firma);
        return new String(texto).equals(decodeFirma.getFirma());
    }
    
    @Override
    public String firmarDocumento(byte[] contenidoDocumento) throws FirmaServiceException {
        return firmarTexto(contenidoDocumento); 
    }

    @Override
    public boolean validarFirmaDocumento(byte[] contenidoDocumento, String firma) throws FirmaServiceException {
        return false;
    }

    @Override
    public InfoCert extraerCertificado(String firma) throws FirmaServiceException {
        FileSystemFirma decode = FileSystemFirma.decode(firma);
        InfoCert info = new InfoCert();
        info.nombrecompleto = decode.getNombre();
        info.nif = decode.getNif();
        return info;
    }
    
    @Override
    public InfoCert extraerCertificadoLogin (String firma) throws FirmaServiceException {
        FileSystemFirma decode = FileSystemFirma.decode(firma);
        InfoCert info = new InfoCert();
        info.nombrecompleto = decode.getNombre();
        info.nif = decode.getNif();
        return info;
    }
    
    @Override
	public Firmante getFirmante(String firma, Documento documento){
		if(firma == null || firma.isEmpty()){
			Messages.error("La firma llegó vacía");
			return null;
		}	
		Firmante firmante = null;
		try {
		    BinaryResponse response = gestorDocumentalService.getDocumento(documento);
			byte[] contenido = response.getBytes();
			firmante = validateXMLSignature(contenido, firma);
			if(firmante == null){
				Messages.error("Error validando la firma");
			}
		} catch (Exception e) {
			play.Logger.error("Error obteniendo el documento del AED para verificar la firma. Uri = " + documento.uri);
			Messages.error("Error validando la firma");
		}
		return firmante;
	}
	
	@Override
	public List<StringArray> getCertInfo(String certificado) throws FirmaServiceException{
		return null;
	}
	
	@Override
	public Firmante validateXMLSignature(byte[] contenidoDoc, String firma) {
		
		Firmante firmante = new Firmante();			
		firmante.idtipo = "nif";
		String idvalor = FirmaController.getIdentificacionFromFirma(firma);
		System.out.println("Firmante con id: "+idvalor);
		if ((idvalor != null) && (!idvalor.isEmpty()))
			firmante.idvalor = idvalor;
		else
			firmante.idvalor = "11111111H";
		firmante.nombre = "Fapito Etsiiano Ulliano";
		return firmante;

	}
	
	@Override
	public void firmar(Documento documento, List<Firmante> firmantes, String firma, String valorDocumentofirmanteSolicitado){
	}

	@Override
	public Firmante getFirmante(String firma, Documento documento, List<Firmante> todosFirmantes) {
		if(firma == null || firma.isEmpty()){
			Messages.error("La firma llegó vacía");
			return null;
		}	
		Firmante firmante = null;
		try {
		    BinaryResponse response = gestorDocumentalService.getDocumento(documento);
			byte[] contenido = response.getBytes();
			firmante = validateXMLSignature(contenido, firma, todosFirmantes);
			if(firmante == null){
				Messages.error("Error validando la firma");
			}
		} catch (Exception e) {
			play.Logger.error("Error obteniendo el documento del AED para verificar la firma. Uri = " + documento.uri);
			Messages.error("Error validando la firma");
		}
		return firmante;
	}

	@Override
	public Firmante validateXMLSignature(byte[] contenidoDoc, String firma, List<Firmante> todosFirmantes) {
		Firmante firmante = null;		
		String identificadorFirmante = FirmaController.getIdentificacionFromFirma(firma);
		if ((identificadorFirmante == null) || (identificadorFirmante.isEmpty()))
			identificadorFirmante="11111111H";
		for (Firmante firmanteAux: todosFirmantes){
    		if (firmanteAux.idvalor.equals(identificadorFirmante)){
    			firmante = firmanteAux;
    			break;
    		}
    	}
		if (firmante == null){
			log.error("Error en validateXMLSignature, Firmante NIF no encontrado: "+identificadorFirmante+" en la lista de firmantes.");
			Messages.error("Error al recuperar el firmante físico.");
			return null;
		}

		System.out.println("Firmante con id: "+identificadorFirmante);

		return firmante;
	}

	@Override
	public String firmarEnServidor(Documento documento)
			throws FirmaServiceException {
		return "FileSystemFirmaService";
	}
 
}
