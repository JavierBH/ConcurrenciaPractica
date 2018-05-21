package cc.qp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import es.upm.babel.cclib.*;

public class QuePasaMonitor implements QuePasa {
	private Map<String, ArrayList<Integer>> miembros = new HashMap<String, ArrayList<Integer>>();
	private Map<String, Integer> creador = new HashMap<String, Integer>();
	private Map<Integer, LinkedList<Mensaje>> mensaje = new HashMap<Integer, LinkedList<Mensaje>>();
	private Map<Integer, LinkedList<Monitor.Cond>> conditions = new HashMap<Integer, LinkedList<Monitor.Cond>>();
	private Monitor mutex;
	private Monitor.Cond hay_mensaje;

	public QuePasaMonitor() {
		mutex = new Monitor();
		hay_mensaje = mutex.newCond();
	}

	@Override
	public void crearGrupo(int creadorUid, String grupo) throws PreconditionFailedException {
		mutex.enter();
		if (creador.containsKey(grupo)) {
			mutex.leave();
			throw new PreconditionFailedException();
		}
		creador.put(grupo, creadorUid);
		ArrayList<Integer> miembros_lista = new ArrayList<Integer>();
		miembros_lista.add(creadorUid);
		miembros.put(grupo, miembros_lista);
		if (mensaje.get(creadorUid) == null) {
			LinkedList<Mensaje> nuevo = new LinkedList<Mensaje>();
			mensaje.put(creadorUid, nuevo);
		}
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
		// Esto puede producir error en MandarMensaje
		miembros.remove(grupo);
		miembros.put(grupo, listaActualizada);
		LinkedList<Mensaje> nuevo = new LinkedList<Mensaje>();
		mensaje.put(nuevoMiembroUid, nuevo);
		mutex.leave();
	}

	@Override
	public void salirGrupo(int miembroUid, String grupo) throws PreconditionFailedException {
		mutex.enter();
		if ((creador.get(grupo) == null || miembros.get(grupo) == null)
				|| (!miembros.get(grupo).contains(miembroUid) && !creador.get(grupo).equals(miembroUid))) {
			mutex.leave();
			throw new PreconditionFailedException();
		}

		if (miembroUid != 0) {
			miembroUid--;
		}
		LinkedList<Mensaje> borrados = mensaje.get(miembroUid);
		for (int i = 0; i < borrados.size(); i++) {
			if (borrados.get(i).getGrupo().equals(grupo)) {
				borrados.remove(i);
			}
		}

		// Error, indexOutOfBounds
		mensaje.remove(miembroUid);
		mensaje.put(miembroUid, borrados);
		ArrayList<Integer> listaActualizada = miembros.get(grupo);

		listaActualizada.remove(miembroUid);
		miembros.remove(grupo);
		miembros.put(grupo, listaActualizada);
		mutex.leave();
	}

	@Override
	public void mandarMensaje(int remitenteUid, String grupo, Object contenidos) throws PreconditionFailedException {
		mutex.enter();
		// Error, NullPointerException
		if (miembros.get(grupo) == null || miembros.get(grupo).contains(remitenteUid)) {
			mutex.leave();
			throw new PreconditionFailedException();
		}

		ArrayList<Integer> n_miembros = miembros.get(grupo);
		Mensaje msge = new Mensaje(remitenteUid, grupo, contenidos);
		for (int i = 0; i < n_miembros.size(); i++) {
			LinkedList<Mensaje> aux = mensaje.get(n_miembros.get(i));
			aux.addLast(msge);
			mensaje.put(n_miembros.get(i), aux);
			desbloquear(n_miembros.get(i));
		}
		mutex.leave();
	}

	@Override
	public Mensaje leer(int uid) {
		mutex.enter();

		if (mensaje.get(uid) == null || mensaje.get(uid).isEmpty()) {
			// Se crea la condicion y se almacena en el Map
			Monitor.Cond aux = mutex.newCond();
			// Si el la entrada del map esta vacia
			if (this.conditions.get(uid) == null) {
				LinkedList<Monitor.Cond> ConditionList = new LinkedList<Monitor.Cond>();
				ConditionList.addLast(aux);
				this.conditions.put(uid, ConditionList);
			} else {
				LinkedList<Monitor.Cond> ConditionList = this.conditions.get(uid);
				ConditionList.addLast(aux);
				this.conditions.replace(uid, ConditionList);
			}

			this.conditions.get(uid).getLast().await();

			// Codigo que se ejcuta una vez dado el signal, elimina la condition

			this.conditions.get(uid).removeLast();
			// Si esta vacia la linkedList se elimina la entrada de la tabla
			if (this.conditions.get(uid).isEmpty()) {
				this.conditions.remove(uid);
			}
		}

		LinkedList<Mensaje> aux = mensaje.get(uid);
		Mensaje msge = aux.getFirst();
		aux.removeFirst();
		mensaje.remove(uid);
		mensaje.put(uid, aux);
		mutex.leave();
		return msge;
	}

	public void desbloquear(int uid) {
		if (!(mensaje.get(uid) == null || mensaje.get(uid).isEmpty())) {
			this.conditions.get(uid).getLast().signal();
		}
	}
}
