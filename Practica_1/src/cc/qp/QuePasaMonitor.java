package cc.qp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import es.upm.babel.cclib.*;

public class QuePasaMonitor implements QuePasa {
	private Map<String,ArrayList<Integer>> miembros= new HashMap<String,ArrayList<Integer>>();
	private Map<String,Integer> creador= new HashMap<String,Integer>();
	private Map<Integer,LinkedList<Object>> mensaje=new HashMap<Integer,LinkedList<Object>>();
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
		mutex.enter();
		if(creador.containsKey(grupo)) { 
			mutex.leave();
			throw new PreconditionFailedException();}
		creador.put(grupo, creadorUid);
		ArrayList<Integer> miembros_lista=new ArrayList<Integer>();
		miembros_lista.add(creadorUid);
		miembros.put(grupo, miembros_lista);
		mutex.leave();
		

	}

	@Override
	public void anadirMiembro(int creadorUid, String grupo, int nuevoMiembroUid) throws PreconditionFailedException {
		mutex.enter();
		if(!creador.containsValue(creadorUid)||miembros.get(grupo).contains(nuevoMiembroUid)) {
			mutex.leave();
			throw new PreconditionFailedException();}
		ArrayList<Integer> listaActualizada=miembros.get(grupo);
		listaActualizada.add(nuevoMiembroUid);
		miembros.replace(grupo, listaActualizada);
		mutex.leave();
	}

	@Override
	public void salirGrupo(int miembroUid, String grupo) throws PreconditionFailedException {
		mutex.enter();
		if(!miembros.get(grupo).contains(miembroUid)||creador.get(grupo).equals(miembroUid)) {
			mutex.leave();
			throw new PreconditionFailedException();}
		ArrayList<Integer> listaActualizada=miembros.get(grupo);
		listaActualizada.remove(miembroUid);
		miembros.replace(grupo, listaActualizada);
		mutex.leave();
	}

	@Override
	public void mandarMensaje(int remitenteUid, String grupo, Object contenidos) throws PreconditionFailedException {
		mutex.enter();
		if(!miembros.get(grupo).contains(remitenteUid)) {
		mutex.leave();
		throw new PreconditionFailedException();}
		ArrayList<Integer> n_miembros=miembros.get(grupo);
		for(int i=0;i<n_miembros.size();i++) {
			LinkedList<Object> aux = mensaje.get(n_miembros.get(i));
			aux.add(contenidos);
			mensaje.put(n_miembros.get(i),aux);
		}
		mutex.leave();
	}

	@Override
	public Mensaje leer(int uid) {
		mutex.enter();
		if (mensaje.isEmpty()) mutex.leave();
		mensaje.remove(mensaje.get(uid));
		mensaje.remove(uid);
		mensaje.remove(miembros.get(uid));
		mutex.leave();
		return null;

	}
	public class Tripla<T, U, V> {

	    private final T first;
	    private final U second;
	    private final V third;

	    public Tripla(T first, U second, V third) {
	        this.first = first;
	        this.second = second;
	        this.third = third;
	    }

	    public T getFirst() { return first; }
	    public U getSecond() { return second; }
	    public V getThird() { return third; }
	}
}
