package cc.qp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import es.upm.babel.cclib.*;

public class QuePasaMonitor implements QuePasa {
	private Map<String, ArrayList<Integer>> miembros = new HashMap<String, ArrayList<Integer>>();
	private Map<String, Integer> creador = new HashMap<String, Integer>();
	private Map<Integer, ArrayList<Object>> mensaje = new HashMap<Integer, ArrayList<Object>>();
	private Monitor mutex;
	private Monitor mutex_mensaje;
	// Todavía no se cuantas conditions poner
	private Monitor.Cond condition;

	public QuePasaMonitor() {
		mutex = new Monitor();
		mutex_mensaje = new Monitor();
		condition = mutex.newCond();
	}

	@Override
	public void crearGrupo(int creadorUid, String grupo) throws PreconditionFailedException {
		// Si el grupo ya está creado devuelve un error
		mutex.enter();
		if (creador.containsKey(grupo)) {
			mutex.leave();
			throw new PreconditionFailedException();
		}
		creador.put(grupo, creadorUid);
		ArrayList<Integer> miembros_lista = new ArrayList<Integer>();
		miembros_lista.add(creadorUid);
		miembros.put(grupo, miembros_lista);
		mutex.leave();

	}

	@Override
	public void anadirMiembro(int creadorUid, String grupo, int nuevoMiembroUid) throws PreconditionFailedException {
		mutex.enter();
		if (!creador.containsValue(creadorUid) || miembros.get(grupo).contains(nuevoMiembroUid)) {
			mutex.leave();
			throw new PreconditionFailedException();
		}
		ArrayList<Integer> listaActualizada = miembros.get(grupo);
		listaActualizada.add(nuevoMiembroUid);
		miembros.replace(grupo, listaActualizada);
		mutex.leave();
	}

	@Override
	public void salirGrupo(int miembroUid, String grupo) throws PreconditionFailedException {
		mutex.enter();
		if (!miembros.get(grupo).contains(miembroUid) || creador.get(grupo).equals(miembroUid)) {
			mutex.leave();
			throw new PreconditionFailedException();
		}
		ArrayList<Integer> listaActualizada = miembros.get(grupo);
		listaActualizada.remove(miembroUid);
		miembros.replace(grupo, listaActualizada);
		mutex.leave();
	}

	@Override
	public void mandarMensaje(int remitenteUid, String grupo, Object contenidos) throws PreconditionFailedException {
		mutex.enter();
		if (!miembros.get(grupo).contains(remitenteUid)) {
			mutex.leave();
			throw new PreconditionFailedException();
		}
		ArrayList<Integer> n_miembros = miembros.get(grupo);
		for (int i = 0; i < n_miembros.size(); i++) {
			ArrayList<Object> aux = mensaje.get(n_miembros.get(i));
			if (aux != null) {
				aux.add(contenidos);
				mensaje.put(n_miembros.get(i), aux);
				if (condition.waiting()<2)condition.signal();
			} else {
				ArrayList<Object> aux2 = new ArrayList<Object>();
				aux2.add(n_miembros.get(i));
				mensaje.put(n_miembros.get(i), aux2);
			}
		}
		mutex.leave();
	}

	@Override
	public Mensaje leer(int uid) {
		mutex.enter();
		mensaje.remove(uid);
		ArrayList<Object> aux = mensaje.get(uid);
		condition.await();
		mutex.leave();
		return (Mensaje) aux.get(aux.size() - 1);
	}
}
