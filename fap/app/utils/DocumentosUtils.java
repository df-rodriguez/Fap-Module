package utils;

import java.util.ArrayList;
 
import models.TipoDocumento;
import models.Tramite;

import properties.FapProperties;
import verificacion.ObligatoriedadDocumentosFap;
 
 public class DocumentosUtils {

	/**
	 * Indica si el tipo de documento es múltiple mirando en todos los trámites del procedimiento. Con que haya uno de tipo multiple, ya asumirá que será multiple, da igual que hay otro de otro tramite que sea UNICO.
	 * 
	 * @param tipoUri
	 * @return
	 */
	static public boolean esTipoMultiple (String tipoUri) {
		TipoDocumento td = TipoDocumento.find("select td from TipoDocumento td where td.uri=? and td.cardinalidad=?", tipoUri, "MULTIPLE").first();
		if (td != null) {
			return true;
 		}
 		return false;
 	}
	
	/**
	 * Indica si el tipo de documento es obligatorio.
	 * 
	 * @param tipoUri
	 * @return
	 */
	static public boolean esTipoObligatorio (String tipoUri) {
		TipoDocumento td = TipoDocumento.find("select td from TipoDocumento td where td.uri=? and td.obligatoriedad=?", tipoUri, "OBLIGATORIO").first();
		if (td != null) {
			return true;
 		}
 		return false;
 	}
	
	/**
	 * Indica si el tipo de documento es imprescindible.
	 * 
	 * @param tipoUri
	 * @return
	 */
	static public boolean esTipoImprescindible (String tipoUri) {
		TipoDocumento td = TipoDocumento.find("select td from TipoDocumento td where td.uri=? and td.obligatoriedad=?", tipoUri, "IMPRESCINDIBLE").first();
		if (td != null) {
			return true;
 		}
 		return false;
 	}
	
	/**
	 * Indica si el tipo de documento es condicionado manual.
	 * 
	 * @param tipoUri
	 * @return
	 */
	static public boolean esTipoCondicionadoManual (String tipoUri) {
		TipoDocumento td = TipoDocumento.find("select td from TipoDocumento td where td.uri=? and td.obligatoriedad=?", tipoUri, "CONDICIONADO_MANUAL").first();
		if (td != null) {
			return true;
 		}
 		return false;
 	}
	
	/**
	 * Indica si el tipo de documento es condicionado automatico.
	 * 
	 * @param tipoUri
	 * @return
	 */
	static public boolean esTipoCondicionadoAutomatico (String tipoUri) {
		TipoDocumento td = TipoDocumento.find("select td from TipoDocumento td where td.uri=? and td.obligatoriedad=?", tipoUri, "CONDICIONADO_AUTOMATICO").first();
		if (td != null) {
			return true;
 		}
 		return false;
 	}
	
	/**
	 * Devuelve el tipo de obligatoriedad del tipo de documento
	 * 
	 * @param tipoUri
	 * @return
	 */
	static public String getTipoObligatoriedad (String tipoUri) {
		TipoDocumento td = TipoDocumento.find("select td from TipoDocumento td where td.uri=?", tipoUri).first();
		if (td != null) {
			return td.obligatoriedad;
 		}
 		return null;
 	}
	
	/**
	 * Indica si el tipo de documento es aportado por el ciudadano.
	 * 
	 * @param tipoUri
	 * @return
	 */
	static public boolean esAportadoCiudadano (String tipoUri) {
		TipoDocumento td = TipoDocumento.find("select td from TipoDocumento td where td.uri=? and td.aportadoPor=?", tipoUri, "CIUDADANO").first();
		if (td != null) {
			return true;
 		}
 		return false;
 	}
	
	/**
	 * Indica si el tipo de documento es aportado por el organismo.
	 * 
	 * @param tipoUri
	 * @return
	 */
	static public boolean esAportadoOrganismo (String tipoUri) {
		TipoDocumento td = TipoDocumento.find("select td from TipoDocumento td where td.uri=? and td.aportadoPor=?", tipoUri, "ORGANISMO").first();
		if (td != null) {
			return true;
 		}
 		return false;
 	}
	
	/**
	 * Indica por quien debe ser aportado el tipo de documento
	 * 
	 * @param tipoUri
	 * @return
	 */
	static public String getAportadoPor (String tipoUri) {
		TipoDocumento td = TipoDocumento.find("select td from TipoDocumento td where td.uri=?", tipoUri).first();
		if (td != null) {
			return td.aportadoPor;
 		}
 		return null;
 	}
	
	/**
	 * Indica cual es el tramite al que pertenece el tipo de documento
	 * 
	 * @param tipoUri
	 * @return
	 */
	static public Tramite getTramitePertenece (String tipoUri) {
		TipoDocumento td = TipoDocumento.find("select td from TipoDocumento td where td.uri=?", tipoUri).first();
		if (td != null) {
			if (td.uri != null)
				return Tramite.find("select t from Tramite t where t.uri=?", td.uri).first();
 		}
 		return null;
 	}
}