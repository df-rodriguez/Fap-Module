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

@Auditable
@Entity
public class Participacion extends FapModel {
	// Código de los atributos

	@ManyToOne(fetch = FetchType.LAZY)
	public Agente agente;

	@ManyToOne(fetch = FetchType.LAZY)
	public SolicitudGenerica solicitud;

	@ValueFromTable("TiposParticipacion")
	public String tipo;

	public void init() {

		postInit();
	}

	// === MANUAL REGION START ===
	// === MANUAL REGION END ===

}
