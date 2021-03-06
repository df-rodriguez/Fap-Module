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
public class CEconomicoSolicitanteManual extends FapModel {
	// Código de los atributos

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	public TipoCEconomico tipo;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinTable(name = "ceconomicosolicitantemanual_valores")
	public List<ValoresCEconomico> valores;

	@Column(columnDefinition = "LONGTEXT")
	public String comentariosAdministracion;

	@Column(columnDefinition = "LONGTEXT")
	public String comentariosSolicitante;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinTable(name = "ceconomicosolicitantemanual_ceconomicosmanuales")
	public List<CEconomico> ceconomicosManuales;

	public CEconomicoSolicitanteManual() {
		init();
	}

	public void init() {

		if (tipo == null)
			tipo = new TipoCEconomico();
		else
			tipo.init();

		if (valores == null)
			valores = new ArrayList<ValoresCEconomico>();

		if (ceconomicosManuales == null)
			ceconomicosManuales = new ArrayList<CEconomico>();

		postInit();
	}

	// === MANUAL REGION START ===

	// === MANUAL REGION END ===

}
