package cc.qp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import es.upm.babel.cclib.*;

public class QuePasaMonitor implements QuePasa {
	private Map<String, ArrayList<Integer>> miembros = new HashMap<String, ArrayList<Integer>>();
	private Map<String, Integer> creador = new HashMap<String, Integer>();
	private Map<Integer, ArrayList<Mensaje>> mensaje = new HashMap<Integer, ArrayList<Mensaje>>();
	private Monitor mutex;
	// Todavía no se cuantas hay_mensajes poner
	private Monitor.Cond hay_mensaje;
	private Monitor.Cond todos_mensajes;
	public QuePasaMonitor() {
		mutex = new Monitor();
		hay_mensaje = mutex.newCond();
		todos_mensajes = mutex.newCond();
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
		ArrayList<Mensaje> borrar=mensaje.get(miembroUid);
		for(int i=0;i<borrar.size();i++) {
			if(borrar.get(i).getGrupo()==grupo) {
				borrar.remove(i);
			}
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
		Mensaje msge = new Mensaje(remitenteUid, grupo, contenidos);
		for (int i = 0; i < n_miembros.size(); i++) {
			ArrayList<Mensaje> aux = mensaje.get(n_miembros.get(i));
			if (aux != null) {
				aux.add(msge);
				mensaje.put(n_miembros.get(i), aux);
				if (hay_mensaje.waiting() > 0) {
					hay_mensaje.signal();
				}
			} else {
				ArrayList<Mensaje> aux2 = new ArrayList<Mensaje>();
				aux2.add(msge);
				mensaje.put(n_miembros.get(i), aux2);
				if (hay_mensaje.waiting() > 0) {
					hay_mensaje.signal();
				}
			}
		}
		this.todos_mensajes.signal();
		mutex.leave();
	}

	@Override
	public Mensaje leer(int uid) {
		mutex.enter();
		if (mensaje.get(uid) == null){
			this.todos_mensajes.await();
			hay_mensaje.await();
		}
		ArrayList<Mensaje> aux = mensaje.get(uid);
		Mensaje msge = aux.get(aux.size() - 1);
		mensaje.remove(uid);
		mutex.leave();
		return msge;
	}

}
