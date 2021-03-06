package models;

import java.util.*;
import javax.persistence.*;
import play.Logger;
import play.db.jpa.JPA;
import play.db.jpa.Model;
import play.data.validation.*;
import org.joda.time.DateTime;
import models.*;
import messages.Messages;
import validation.*;
import audit.Auditable;
import java.text.ParseException;
import java.text.SimpleDateFormat;

// === IMPORT REGION START ===

// === IMPORT REGION END ===

@Entity
public class Documentacion extends FapModel {
	// Código de los atributos

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinTable(name = "documentacion_documentos")
	public List<Documento> documentos;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinTable(name = "documentacion_documentosexternos")
	public List<DocumentoExterno> documentosExternos;

	public String uriDocOficial;

	public String urlDocOficial;

	public Boolean docOficialClasificado;

	@org.hibernate.annotations.Columns(columns = { @Column(name = "fechaFirma"), @Column(name = "fechaFirmaTZ") })
	@org.hibernate.annotations.Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTimeWithZone")
	public DateTime fechaFirma;

	@Transient
	public String firma;

	public Documentacion() {
		init();
	}

	public void init() {

		if (documentos == null)
			documentos = new ArrayList<Documento>();

		if (documentosExternos == null)
			documentosExternos = new ArrayList<DocumentoExterno>();

		postInit();
	}

	// === MANUAL REGION START ===

	// === MANUAL REGION END ===

}
