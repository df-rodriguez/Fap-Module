package templates;

import es.fap.simpleled.led.*;
import es.fap.simpleled.led.util.LedEntidadUtils
import generator.utils.CampoUtils
import generator.utils.Entidad;
import generator.utils.StringUtils
import org.eclipse.emf.ecore.EObject;


public class GSaveCampoElement extends GElement {

	public CampoUtils campo;
	
	public GSaveCampoElement(EObject element, GElement container){
		super(element, container);
	}
	
	public Set<Entidad> saveEntities(){
		Set<Entidad> saveEntities = super.saveEntities();
		if (campo?.campo?.entidad != null)
			saveEntities.add(Entidad.create(campo.campo.entidad));
		return saveEntities;	
	}
	
	private static String valid(String campo, Stack<Set<String>> validatedFields){
		campo = StringUtils.firstLower(campo);
		for (Set<String> set: validatedFields){
			if (set.contains(campo))
				return "";
		}
		validatedFields.peek().add(campo);
		return "CustomValidation.valid(\"${campo}\", ${campo});\n";
	}
	
	private static String required(CampoUtils campo){
		return "CustomValidation.required(\"${campo.firstLower()}\", ${campo.firstLower()});\n";
	}
	
	public String validateCopy(Stack<Set<String>> validatedFields){
		return validate(validatedFields) + copy();
	}
	
	public String validate(Stack<Set<String>> validatedFields){
		if (campo.isMethod())
			return "";
		String validation = "";
		String campoStr = StringUtils.firstLower(campo.str);
		if (LedEntidadUtils.getEntidad(campo.getUltimoAtributo()))
			validation += valid(campo.str, validatedFields);
		int dotPlace = campoStr.lastIndexOf('.', campoStr.length() - 1);
		while (dotPlace != -1) {
			validation += valid(campoStr.substring(0, dotPlace), validatedFields);
			dotPlace = campoStr.lastIndexOf('.', dotPlace - 1);
		}
		if (element.metaClass.respondsTo(element, "isRequerido") && element.isRequerido())
			validation += required(campo);
		return validation;
	}

	public String copy(){
		// Si el campo es un ManyToOne o ManyToMany, solo se copia la referencia.
		if (LedEntidadUtils.isManyToOne(campo.getUltimoAtributo()))
			return copyCampoMany2One(campo);
		if (LedEntidadUtils.isManyToMany(campo.getUltimoAtributo()))
			return copyCampoMany2Many(campo);
		return copyCampos(campo);
	}
	
	public String bindReferences(){
		if (LedEntidadUtils.isManyToOne(campo.getUltimoAtributo()))
			return bindCampoMany2One(campo);
		if (LedEntidadUtils.isManyToMany(campo.getUltimoAtributo())) 
			return bindCampoMany2Many(campo);
		return "";
	}
	
	public static String bindCampoMany2One(CampoUtils campo) {
		String entity = campo.getUltimaEntidad().name;
		String str_ = campo.getStr_();
		return """
			String ${str_} = params.get("$str_");
			if ((${str_} != null) && (!${str_}.trim().equals(""))) {
				$entity ${str_}ctr = ${entity}.findById(Long.parseLong(${str_}.trim()));
				${campo.firstLower()} = ${str_}ctr;
			}
			else
				${campo.firstLower()} = null;
		""";
	}

	public static String bindCampoMany2Many(CampoUtils campo) {
		String entity = campo.getUltimaEntidad().name;
		String str_ = campo.getStr_();
		return """
			ArrayList<$entity> ${str_}aCT = new ArrayList<$entity>();
			String[] $str_ = params.getAll("$str_");
			if ($str_ != null) {
				for (String idString : $str_) {
					$entity ctr = ${entity}.findById(Long.parseLong(idString.trim()));
					${str_}aCT.add(ctr);
				}
			}
			${campo.firstLower()} = ${str_}aCT;
		""";
	}
	
	public static String copyCampoMany2One(CampoUtils campo) {
		return "db${campo.str} = ${campo.firstLower()};";
	}

	public static String copyCampoMany2Many(CampoUtils campo) {
		return """
			db${campo.str}.retainAll(${campo.firstLower()});
			db${campo.str}.addAll(${campo.firstLower()});
		""";
	}
	
	public static String copyCampos(CampoUtils campo) {
		Entity entidad;
		Attribute last = campo.getUltimoAtributo();
		if (last != null){
			if (LedEntidadUtils.xToMany(last))
				return "";
			entidad = LedEntidadUtils.getEntidad(last);
		}
		else
			entidad = campo.entidad.entidad;
		if (entidad != null){
			String copy = "";
			for (Attribute at: LedEntidadUtils.getAllDirectAttributesExceptId(entidad))
				copy += copyCampos(campo.addAttribute(at));
			return copy;
		}
		else
			return copyCampo(campo);
	}
	
	public static String copyCampo(CampoUtils campo) {
		if (campo.isMethod())
			return "";
		if (LedEntidadUtils.isManyToOne(campo.getUltimoAtributo()))
			return copyCampoMany2One(campo);
		else if (LedEntidadUtils.isManyToMany(campo.getUltimoAtributo()))
			return copyCampoMany2Many(campo);
		else if (campo.getUltimoAtributo()?.type.compound?.multiple){
			return """
			db${campo.str}.retainAll(${campo.firstLower()});
			db${campo.str}.addAll(${campo.firstLower()});
			"""
		}
		return "db${campo.str} = ${campo.firstLower()};\n";
	}

	public static String copyCamposFiltrados(CampoUtils campo, List<String> subcampos){
		String copy = "";
		for (String campoStr: subcampos){
			CampoUtils c = CampoUtils.create(campo.addMore(campoStr).campo);
			if (c != null)
				copy += copyCampo(c);
		}
		return copy;
	}
}
