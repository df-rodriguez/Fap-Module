package controllers;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;

import oauth.signpost.http.HttpRequest;

import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.ws.security.handler.RequestData;

import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Util;
import play.utils.HTML;
import properties.FapProperties;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.ning.http.client.RequestBuilder;
import com.sun.net.httpserver.HttpContext;

import messages.Messages;
import models.Aplicacion;
import models.ServicioWebInfo;
import models.ServiciosWeb;
import controllers.gen.ServiciosWebAppControllerGen;

public class ServiciosWebAppController extends ServiciosWebAppControllerGen {
	
	public static void index(String accion, Long idAplicacion) {
		if (accion == null)
			accion = getAccion();
		if (!permiso(accion)) {
			Messages.fatal("No tiene permisos suficientes para realizar esta acción");
			renderTemplate("gen/ServiciosWebApp/ServiciosWebApp.html");
		}

		String appName = FapProperties.get("application.name");
		String path = FapProperties.get("http.path");
		Aplicacion aplicacion = Aplicacion.find("select aplicacion from Aplicacion aplicacion where aplicacion.nombreApp=?", appName).first();
		if (aplicacion == null) {
			aplicacion = ServiciosWebAppController.getAplicacion();
			aplicacion.nombreApp = appName;
			String host = Http.Request.current.get().host;
			if (path != null)
				aplicacion.urlApp = "http://" + host + path + "/WSInfo";
			else
				aplicacion.urlApp = "http://" + host + "/WSInfo";
			aplicacion.save();
			
			if (properties.FapProperties.getBoolean("fap.entidades.guardar.antes")) {
				aplicacion.save();
				idAplicacion = aplicacion.id;
				accion = "editar";
			}
		}
		
		idAplicacion = aplicacion.id;
		if (aplicacion.serviciosWeb.size() == 0)
			getWS(accion, idAplicacion, aplicacion);
		else {
			log.info("Visitando página: " + "gen/ServiciosWebApp/ServiciosWebApp.html");
			renderTemplate("gen/ServiciosWebApp/ServiciosWebApp.html", accion, idAplicacion, aplicacion);
		}
			
	}
	
	@Util
	public static void getWS(String accion, Long idAplicacion, Aplicacion aplicacion) {
		
		String urlApp = aplicacion.urlApp;
		WSRequest request = null;
		JsonElement json = null;
		
		try {
			request = WS.url(urlApp);
			json = request.get().getJson();
		} catch (RuntimeException ce) {
			Messages.warning("El servicio web no está disponible en estos momentos");
			play.Logger.error("El servicio web no está disponible en estos momentos");
		} 
		
		if (json != null) {
			JsonArray array = json.getAsJsonArray();		
			Gson gson = new Gson();
			int i = 0;
			
			while (i < array.size()) {
				ServiciosWeb servicioWeb = new ServiciosWeb();
				servicioWeb.servicioWebInfo = gson.fromJson(array.get(i), ServicioWebInfo.class);
				servicioWeb.servicioWebInfo.activo = true;
				servicioWeb.save();
				aplicacion.serviciosWeb.add(servicioWeb);
				aplicacion.save();
				i++;
			}
		}
		log.info("Visitando página: " + "gen/ServiciosWebApp/ServiciosWebApp.html");
		renderTemplate("gen/ServiciosWebApp/ServiciosWebApp.html", accion, idAplicacion, aplicacion);
	
	}
	
	@Util
	public static void formBtnRecargaWS(Long idAplicacion, String recargasWS) {
		checkAuthenticity();
		if (!permisoFormBtnRecargaWS("editar")) {
			Messages.error("No tiene permisos suficientes para realizar la acción");
		}

		if (!Messages.hasErrors()) {
			recargaWS(idAplicacion);
		}

		if (!Messages.hasErrors()) {
			ServiciosWebAppController.formBtnRecargaWSValidateRules();
		}
		if (!Messages.hasErrors()) {

			log.info("Acción Editar de página: " + "gen/ServiciosWebApp/ServiciosWebApp.html" + " , intentada con éxito");
		} else
			log.info("Acción Editar de página: " + "gen/ServiciosWebApp/ServiciosWebApp.html" + " , intentada sin éxito (Problemas de Validación)");
		ServiciosWebAppController.formBtnRecargaWSRender(idAplicacion);
	}
	
	@Util
	public static void recargaWS(Long idAplicacion) {
		
		Aplicacion app = getAplicacion(idAplicacion);
		String urlApp = app.urlApp;
		WSRequest request = null;
		JsonElement json = null;
		try {
			request = WS.url(urlApp);
			json = request.get().getJson();
		} catch (RuntimeException ce) {
			Messages.warning("El servicio web no está disponible en estos momentos");
			play.Logger.error("El servicio web no está disponible en estos momentos");
		}
		
		if (json != null) {
			JsonArray array = json.getAsJsonArray();		
			int i = 0;
			List<ServiciosWeb> anteriorServicioWeb = ServiciosWeb.find("select serviciosWeb from Aplicacion aplicacion join aplicacion.serviciosWeb serviciosWeb where aplicacion.id=? and serviciosWeb.servicioWebInfo.activo=true", idAplicacion).fetch();
			int numWS = array.size();
			int anteriorNumWS = anteriorServicioWeb.size();
			if (numWS < anteriorNumWS) {
				while (i < numWS) {
					diferentesWS(array, i, anteriorServicioWeb, app);
					i++;
				}
				for (int j = i; j < anteriorNumWS; j++) {
					anteriorServicioWeb.get(j).servicioWebInfo.activo = false;
					anteriorServicioWeb.get(j).servicioWebInfo.save();
				}
			} else {
				i = 0;
				if (numWS > anteriorNumWS) {
					while (i < anteriorNumWS) {
						diferentesWS(array, i, anteriorServicioWeb, app);
						i++;
					}
					for (int j = i; j < numWS; j++) {
						Gson gson = new Gson();
						ServiciosWeb servicioWeb = new ServiciosWeb();
						servicioWeb.servicioWebInfo = gson.fromJson(array.get(j), ServicioWebInfo.class);
						servicioWeb.save();
						servicioWeb.servicioWebInfo.activo = true;
						servicioWeb.servicioWebInfo.save();
						app.serviciosWeb.add(servicioWeb);
						app.save();
					}
				}
				else {
					while (i < anteriorNumWS) {
						diferentesWS(array, i, anteriorServicioWeb, app);
						i++;
					}
				}
			}
		}
	}
	
	@Util
	public static void diferentesWS(JsonArray array, int i, List<ServiciosWeb> serviciosWeb, Aplicacion app) {
	
		Gson gson = new Gson();
		ServicioWebInfo anteriorServicioWebInfo = serviciosWeb.get(i).servicioWebInfo;
		ServicioWebInfo actualServicioWebInfo = gson.fromJson(array.get(i), ServicioWebInfo.class);
	
		if (((!anteriorServicioWebInfo.nombre.equals(actualServicioWebInfo.nombre)) ||
			(!anteriorServicioWebInfo.urlWS.equals(actualServicioWebInfo.urlWS)))) {
	
			anteriorServicioWebInfo.activo = false;
			anteriorServicioWebInfo.save();
			actualServicioWebInfo.activo= true;
			actualServicioWebInfo.save();
			ServiciosWeb ws = new ServiciosWeb();
			ws.servicioWebInfo = actualServicioWebInfo;
			ws.save();
			app.serviciosWeb.add(ws);
			app.save();
		}
		else {
			int infoAnterior = anteriorServicioWebInfo.infoParams.size();
			int infoActual = actualServicioWebInfo.infoParams.size();
			if (infoAnterior < infoActual) {
				anteriorServicioWebInfo.activo = false;
				anteriorServicioWebInfo.save();
				actualServicioWebInfo.activo= true;
				actualServicioWebInfo.save();
				ServiciosWeb ws = new ServiciosWeb();
				ws.servicioWebInfo = actualServicioWebInfo;
				ws.save();
				app.serviciosWeb.add(ws);
				app.save();
			}
			else {
				if (infoAnterior > infoActual) {
					anteriorServicioWebInfo.activo = false;
					anteriorServicioWebInfo.save();
					actualServicioWebInfo.activo= true;
					actualServicioWebInfo.save();
					ServiciosWeb ws = new ServiciosWeb();
					ws.servicioWebInfo = actualServicioWebInfo;
					ws.save();
					app.serviciosWeb.add(ws);
					app.save();
				}
				else {
					for (int j = 0; j < infoActual; j++) {
						if ((!anteriorServicioWebInfo.infoParams.get(j).nombreParam.equals(actualServicioWebInfo.infoParams.get(j).nombreParam))
							|| (!anteriorServicioWebInfo.infoParams.get(j).tipo.equals(actualServicioWebInfo.infoParams.get(j).tipo))) {
							
							anteriorServicioWebInfo.activo = false;
							anteriorServicioWebInfo.save();
							actualServicioWebInfo.activo= true;
							actualServicioWebInfo.save();
							ServiciosWeb ws = new ServiciosWeb();
							ws.servicioWebInfo = actualServicioWebInfo;
							ws.save();
							app.serviciosWeb.add(ws);
							app.save();
						}
					}
				}
			}
		}
	}
	
//	@Util
//	public static void recargasDatosFormBtnRecargaWS(Long idAplicacion) {
//		
//		Aplicacion app = getAplicacion(idAplicacion);
//		String urlApp = app.urlApp;
//		List<ServiciosWeb> anterioresServiciosWeb = ServiciosWeb.find("select serviciosWeb from Aplicacion aplicacion join aplicacion.serviciosWeb serviciosWeb where aplicacion.id=? and serviciosWeb.servicioWebInfo.activo=true", idAplicacion).fetch();
//		int i = 0;
//		int anteriorNumWS = anterioresServiciosWeb.size();		
//		
//		while (i < anteriorNumWS) {
//			String urlWS = anterioresServiciosWeb.get(i).servicioWebInfo.urlWS;
//			WSRequest request = null;
//			JsonElement json = null;
//			String url = urlApp + urlWS;
//			try {
//				request = WS.url(url);
//				json = request.get().getJson();
//			} catch (RuntimeException ce) {
//				Messages.warning("El servicio web no está disponible en estos momentos");
//				play.Logger.error("El servicio web no está disponible en estos momentos");
//			}
//			
////			if ((json != null) && (anterioresServiciosWeb.get(i).peticion.size() > 0)) {
////				ListaConsultas anterioresConsultas = anterioresServiciosWeb.get(i).peticion.get(0).lista.get(0);
////				Gson gson = new Gson();
////				DatosGraficas nuevaConsulta = gson.fromJson(json, DatosGraficas.class);
////				ListaConsultas nuevasConsulta = nuevaConsulta.lista.get(0);
////				
////				if ((anterioresConsultas.consultasWS.size() < nuevasConsulta.consultasWS.size()) || (anterioresConsultas.consultasWS.size() > nuevasConsulta.consultasWS.size())) {
////					recargasWSFormBtnRecargaWS(idAplicacion);
////				}
////				else {
////					for (int j = 0; j < anterioresConsultas.consultasWS.size(); j++) {
////						ConsultasWS consultaAnt = anterioresConsultas.consultasWS.get(j);
////						ConsultasWS consultaPost = nuevasConsulta.consultasWS.get(j);
////						for (int k = 0; k < consultaAnt.consultaWS.size(); k++) {
////							if ((!consultaAnt.consultaWS.get(k).nombre.equals(consultaPost.consultaWS.get(k).nombre))
////									|| (consultaAnt.consultaWS.get(k).valorBoolean != consultaPost.consultaWS.get(k).valorBoolean)
////									|| (consultaAnt.consultaWS.get(k).valorDouble != consultaPost.consultaWS.get(k).valorDouble)
////									|| (consultaAnt.consultaWS.get(k).valorDateTime != consultaPost.consultaWS.get(k).valorDateTime)
////									|| (consultaAnt.consultaWS.get(k).valorLong != consultaPost.consultaWS.get(k).valorLong)
////									|| (consultaAnt.consultaWS.get(k).valorString != consultaPost.consultaWS.get(k).valorString)) {
////								recargasWSFormBtnRecargaWS(idAplicacion);
////							}
////						}
////					}
////				}
////				
////			}
//			i++;
//		}
//	}
	
	/**
	 * Tabla en la que se muestran los servicios web activos.
	 * @param idAplicacion
	 */
	public static void tablaserviciosWeb(Long idAplicacion) {
		java.util.List<ServiciosWeb> rows = ServiciosWeb.find("select serviciosWeb from Aplicacion aplicacion join aplicacion.serviciosWeb serviciosWeb where aplicacion.id=?", idAplicacion).fetch();
		Map<String, Long> ids = (Map<String, Long>) tags.TagMapStack.top("idParams");
		List<ServiciosWeb> rowsFiltered = new ArrayList<ServiciosWeb>();
		
		for (int i = 0; i < rows.size(); i++) {
			if (rows.get(i).servicioWebInfo.activo)
				rowsFiltered.add(rows.get(i));
		}
		
		tables.TableRenderResponse<ServiciosWeb> response = new tables.TableRenderResponse<ServiciosWeb>(rowsFiltered, false, false, false, "", "", "", getAccion(), ids);
		renderJSON(response.toJSON("servicioWebInfo.nombre", "servicioWebInfo.urlWS", "id"));
	}
	
	/**
	 * Tabla en la que solo se muestra un historial de servicios web que ya no están activos.
	 * @param idAplicacion
	 */
	public static void tablahistorialServiciosWeb(Long idAplicacion) {
		java.util.List<ServiciosWeb> rows = ServiciosWeb.find("select serviciosWeb from Aplicacion aplicacion join aplicacion.serviciosWeb serviciosWeb where aplicacion.id=?", idAplicacion).fetch();
		Map<String, Long> ids = (Map<String, Long>) tags.TagMapStack.top("idParams");
		List<ServiciosWeb> rowsFiltered = new ArrayList<ServiciosWeb>();
		
		for (int i = 0; i < rows.size(); i++) {
			if (!rows.get(i).servicioWebInfo.activo)
				rowsFiltered.add(rows.get(i));
		}
		
		tables.TableRenderResponse<ServiciosWeb> response = new tables.TableRenderResponse<ServiciosWeb>(rowsFiltered, false, false, false, "", "", "", getAccion(), ids);
		renderJSON(response.toJSON("servicioWebInfo.nombre", "servicioWebInfo.urlWS", "id"));
	}
	
}