package com.sma.agentes;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AgenteRecomendador extends Agent {
	private static final double MAX_DISTANCE = Math.sqrt(32.0); // sqrt((5-1)^2 + (5-1)^2)
	private final List<Cancion> catalogo = new ArrayList<>();

	@Override
	protected void setup() {
		String nombre = getLocalName();
		System.out.println("[Recomendador] " + nombre + " iniciando...");

		registrarServicio();
		cargarCatalogo(nombre);

		addBehaviour(new CyclicBehaviour(this) {
			@Override
			public void action() {
				ACLMessage msg = receive();
				if (msg != null && msg.getPerformative() == ACLMessage.CFP) {
					System.out.println("[" + getLocalName() + "] CFP recibido de " + msg.getSender().getLocalName());
					System.out.println("[" + getLocalName() + "] Contenido CFP: " + msg.getContent());

					// Parsear el contexto
					ContextoMusical contexto = extraerContexto(msg.getContent());
                    
					// Buscar mejor canción
					Cancion mejorCancion = buscarMejorCancion(contexto.energia, contexto.mood);
                    
					if (mejorCancion != null) {
						// Calcular similitud
						int puntaje = calcularPuntaje(mejorCancion.energia, mejorCancion.mood, 
													   contexto.energia, contexto.mood);
                        
						// Responder con PROPOSE
						ACLMessage propose = msg.createReply();
						propose.setPerformative(ACLMessage.PROPOSE);
						propose.setContent(mejorCancion.nombre + "|" + puntaje);
						send(propose);
                        
						System.out.println("[" + getLocalName() + "] PROPOSE enviado: " + mejorCancion.nombre + " (" + puntaje + "%)");
					} else {
						// Si no hay canción (catálogo vacío), rechazar
						ACLMessage refuse = msg.createReply();
						refuse.setPerformative(ACLMessage.REFUSE);
						refuse.setContent("Catalogo vacio");
						send(refuse);
						System.err.println("[" + getLocalName() + "] Catálogo vacío, REFUSE enviado");
					}
				} else {
					block();
				}
			}
		});
	}

	@Override
	protected void takeDown() {
		try {
			DFService.deregister(this);
			System.out.println("[" + getLocalName() + "] Desregistrado del DF");
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}

	// ===== Métodos privados =====

	private void registrarServicio() {
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("recomendador-musical");
		sd.setName(getLocalName());
		dfd.addServices(sd);

		try {
			DFService.register(this, dfd);
			System.out.println("[" + getLocalName() + "] Registrado en DF como recomendador-musical");
		} catch (FIPAException fe) {
			System.err.println("[" + getLocalName() + "] Error registrando en DF: " + fe.getMessage());
		}
	}

	private void cargarCatalogo(String nombreAgente) {
		catalogo.clear();
		String nombreNormalizado = nombreAgente.toUpperCase(Locale.ROOT);

		if (nombreNormalizado.contains("A")) {
			// Catálogo Rock
			catalogo.add(new Cancion("Rock Pulse", 5, 4));
			catalogo.add(new Cancion("Electric Road", 5, 3));
			catalogo.add(new Cancion("Riff Central", 4, 2));
			catalogo.add(new Cancion("Night Amplifier", 4, 5));
			catalogo.add(new Cancion("Backstage Riot", 3, 2));
			System.out.println("[" + nombreAgente + "] Especializado en Rock (" + catalogo.size() + " canciones)");
		} else if (nombreNormalizado.contains("B")) {
			// Catálogo Lofi
			catalogo.add(new Cancion("Soft Drift", 2, 5));
			catalogo.add(new Cancion("Lofi Lantern", 1, 4));
			catalogo.add(new Cancion("Study Waves", 2, 5));
			catalogo.add(new Cancion("Warm Loop", 3, 4));
			catalogo.add(new Cancion("Calm Vinyl", 1, 3));
			System.out.println("[" + nombreAgente + "] Especializado en Lofi (" + catalogo.size() + " canciones)");
		} else {
			// Catálogo por defecto
			catalogo.add(new Cancion("Default Track", 3, 3));
			System.out.println("[" + nombreAgente + "] Sin especialización clara, catálogo por defecto");
		}
	}

	private Cancion buscarMejorCancion(int energiaObjetivo, int moodObjetivo) {
		Cancion mejor = null;
		double mejorDistancia = Double.MAX_VALUE;

		for (Cancion cancion : catalogo) {
			double distancia = distanciaEuclidiana(cancion.energia, cancion.mood, energiaObjetivo, moodObjetivo);
			if (distancia < mejorDistancia) {
				mejorDistancia = distancia;
				mejor = cancion;
			}
		}
		return mejor;
	}

	private double distanciaEuclidiana(int e1, int m1, int e2, int m2) {
		int deltaEnergia = e1 - e2;
		int deltaMood = m1 - m2;
		return Math.sqrt((deltaEnergia * deltaEnergia) + (deltaMood * deltaMood));
	}

	private int calcularPuntaje(int energiaCancion, int moodCancion, int energiaObjetivo, int moodObjetivo) {
		double distancia = distanciaEuclidiana(energiaCancion, moodCancion, energiaObjetivo, moodObjetivo);
		double similitud = 1.0 - (distancia / MAX_DISTANCE);
		int puntaje = Math.max(0, (int) Math.round(similitud * 100.0));
		return Math.min(100, puntaje);
	}

	private ContextoMusical extraerContexto(String contenido) {
		if (contenido == null || contenido.trim().isEmpty()) {
			return new ContextoMusical(3, 3); // Valores por defecto
		}

		try {
			// Esperamos formato: "Genero;Energia;Mood"
			String[] partes = contenido.split(";");
			if (partes.length >= 3) {
				int energia = Integer.parseInt(partes[1].trim());
				int mood = Integer.parseInt(partes[2].trim());
				return new ContextoMusical(acotarRango(energia), acotarRango(mood));
			}
		} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
			System.err.println("[" + getLocalName() + "] Error parseando contexto: " + contenido);
		}
        
		return new ContextoMusical(3, 3); // Fallback
	}

	private int acotarRango(int valor) {
		return Math.max(1, Math.min(5, valor));
	}

	// ===== Clases internas =====

	private static class Cancion {
		final String nombre;
		final int energia;
		final int mood;

		Cancion(String nombre, int energia, int mood) {
			this.nombre = nombre;
			this.energia = energia;
			this.mood = mood;
		}

		@Override
		public String toString() {
			return nombre + " (E:" + energia + ", M:" + mood + ")";
		}
	}

	private static class ContextoMusical {
		final int energia;
		final int mood;

		ContextoMusical(int energia, int mood) {
			this.energia = energia;
			this.mood = mood;
		}

		@Override
		public String toString() {
			return "Contexto(E:" + energia + ", M:" + mood + ")";
		}
	}
}
