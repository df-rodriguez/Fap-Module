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
import validation.PasswordCheck;
import validation.PasswordCheck;

// === IMPORT REGION START ===
import play.mvc.Scope.Session;

// === IMPORT REGION END ===

@Auditable
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Agente extends FapModel {
	// Código de los atributos

	public String username;

	public String passwordAntiguo;

	@CheckWith(validation.PasswordCheck.class)
	public String password;

	@CheckWith(validation.PasswordCheck.class)
	@Transient
	public String newpassword;

	@Email
	public String email;

	public String name;

	@ElementCollection
	@ValueFromTable("roles")
	public Set<String> roles;

	@ValueFromTable("roles")
	public String rolActivo;

	public String acceso;

	public Boolean funcionario;

	@Transient
	public String verificacion;

	public Agente() {
		init();
	}

	public void init() {

		if (roles == null)
			roles = new HashSet<String>();

		postInit();
	}

	// === MANUAL REGION START ===

	public String getRolActivo() {
		String rol = null;
		// Issue #98
		// Mantiene el rol activo en sesión
		if (Session.current().contains("role")) {
			rol = Session.current().get("role");
		} else {
			//El rol activo no está en sesión, usa el de db
			cambiarRolActivo(this.rolActivo);
			rol = this.rolActivo;
		}
		return rol;
	}

	public void cambiarRolActivo(String rolActivo) {
		if (rolActivo == null) {
			//El usuario no tiene ningún rolActivo guardado en db
			rolActivo = getFirstRole();
		} else if (!roles.contains(rolActivo)) {
			//El usuario está intentando cambiar a un rol que no tiene
			//Se le asigna el primer de la lista.
			//Se debería lanzar un error?
			rolActivo = getFirstRole();
		}

		//Guarda el último rolActivo en base de datos
		this.rolActivo = rolActivo;
		this.save();

		//Mantiene el rolActivo en sesión
		Session.current().put("role", rolActivo);
	}

	//Devuelve el primer rol de la lista
	private String getFirstRole() {
		String rol;
		if (roles.size() > 0) {
			rol = roles.iterator().next();
		} else {
			//TODO mejorar esta excepción
			throw new RuntimeException("Intentando establecer un rolActivo que no tiene el usuario");
		}
		return rol;
	}

	@Override
	public String toString() {
		return "Agente [username=" + username + ", email=" + email + "]";
	}

	public boolean getFuncionario() {
		if (funcionario != null)
			return funcionario;
		return false;
	}

	/**
	 * Devuelve la lista de roles ordenados alfabéticamente
	 * @return
	 */
	public List<String> getSortRoles() {
		List<String> list = new ArrayList<String>(this.roles);
		Collections.sort(list);
		return list;
	}

	/**
	 * Devuelve true si el agente accedió por certificado
	 * @return
	 */
	public Boolean accedidoPorCertificado() {
		return this.acceso != null && this.acceso.equalsIgnoreCase("certificado");
	}
	// === MANUAL REGION END ===

}
