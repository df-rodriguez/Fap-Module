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
public class ConsultaWS extends FapModel {
	// Código de los atributos

	public String nombre;

	public Integer valorInteger;

	public String valorString;

	public Double valorDouble;

	@org.hibernate.annotations.Columns(columns = { @Column(name = "valorDateTime"), @Column(name = "valorDateTimeTZ") })
	@org.hibernate.annotations.Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTimeWithZone")
	public DateTime valorDateTime;

	public Boolean valorBoolean;

	public void init() {

		postInit();
	}

	// === MANUAL REGION START ===

	public ConsultaWS(String nombre, Integer integer) {
		this.nombre = nombre;
		this.valorInteger = integer;
		this.valorBoolean = null;
		this.valorDateTime = null;
		this.valorDouble = null;
		this.valorString = null;
	}

	public ConsultaWS(String nombre, Boolean bool) {
		this.nombre = nombre;
		this.valorInteger = null;
		this.valorBoolean = bool;
		this.valorDateTime = null;
		this.valorDouble = null;
		this.valorString = null;
	}

	public ConsultaWS(String nombre, DateTime datetime) {
		this.nombre = nombre;
		this.valorInteger = null;
		this.valorBoolean = null;
		this.valorDateTime = datetime;
		this.valorDouble = null;
		this.valorString = null;
	}

	public ConsultaWS(String nombre, Double doubl) {
		this.nombre = nombre;
		this.valorInteger = null;
		this.valorBoolean = null;
		this.valorDateTime = null;
		this.valorDouble = doubl;
		this.valorString = null;
	}

	public ConsultaWS(String nombre, String string) {
		this.nombre = nombre;
		this.valorInteger = null;
		this.valorBoolean = null;
		this.valorDateTime = null;
		this.valorDouble = null;
		this.valorString = string;
	}

	// === MANUAL REGION END ===

}