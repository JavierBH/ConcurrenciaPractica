package cc.qp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import es.upm.babel.cclib.*;

public class QuePasaMonitor implements QuePasa {
	private Map<String,ArrayList<Integer>> miembros= new HashMap<String,ArrayList<Integer>>();
	private Map<String,Integer> creador= new HashMap<String,Integer>();
	private Monitor mutex;
	//Todavía no se cuantas conditions poner
	private Monitor.Cond nosequenombreponer;
	public QuePasaMonitor() {
		mutex=new Monitor();
		nosequenombreponer=mutex.newCond();
	}
	
	@Override
	public void crearGrupo(int creadorUid, String grupo) throws PreconditionFailedException {
		//Si el grupo ya está creado devuelve un error
		if(creador.containsKey(grupo)) 
			throw new PreconditionFailedException();
		creador.put(grupo, creadorUid);
		ArrayList<Integer> miembros_lista=new ArrayList<Integer>();
		miembros_lista.add(creadorUid);
		miembros.put(grupo, miembros_lista);
		

	}

	@Override
	public void anadirMiembro(int creadorUid, String grupo, int nuevoMiembroUid) throws PreconditionFailedException {
		if(!creador.containsValue(creadorUid)||miembros.get(grupo).contains(nuevoMiembroUid))
			throw new PreconditionFailedException();
		ArrayList<Integer> listaActualizada=miembros.get(grupo);
		listaActualizada.add(nuevoMiembroUid);
		miembros.replace(grupo, listaActualizada);
	}

	@Override
	public void salirGrupo(int miembroUid, String grupo) throws PreconditionFailedException {
		if(!miembros.get(grupo).contains(miembroUid)||creador.get(grupo).equals(miembroUid))
			throw new PreconditionFailedException();
		ArrayList<Integer> listaActualizada=miembros.get(grupo);
		listaActualizada.remove(miembroUid);
		miembros.replace(grupo, listaActualizada);
	}

	@Override
	public void mandarMensaje(int remitenteUid, String grupo, Object contenidos) throws PreconditionFailedException {
		

	}

	@Override
	public Mensaje leer(int uid) {
		
		return null;
	}

}
