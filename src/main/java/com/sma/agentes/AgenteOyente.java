package com.sma.agentes;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

public class AgenteOyente extends Agent {
    private String mejorCancion = "";
    private int mejorScore = -1;
    private int respuestasRecibidas = 0;

    @Override
    protected void setup() {
        System.out.println("Oyente [" + getLocalName() + "] buscando servicios...");

        addBehaviour(new WakerBehaviour(this, 15000) {
            @Override
            protected void onWake() {
                // buscar agente analista en páginas amarillas 
                AID analista = buscarServicioLocal("analista-musical");
                if (analista != null) { 
                    //enviar mensaje al agente analista para que inicie el proceso de análisis del contexto musical
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.addReceiver(analista);
                    send(msg);
                }
            }
        });

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage reply = receive();
                if (reply != null) {
                    if (reply.getPerformative() == ACLMessage.INFORM && reply.getContent().contains(";")) {
                        String contexto = reply.getContent();
                        System.out.println("Oyente: Contexto recibido -> [" + contexto + "]. Buscando recomendadores ...");
                        //buscar agentes recomendadores en DF remoto (Docker)
                        AID[] recomendadores = buscarServiciosRemotos("recomendador-musical");
                        for (AID r : recomendadores) {
                            //enviar mensaje  cada agente recomendador con el contexto obtenido por el analista para que realicen su propuesta de canción
                            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                            cfp.setContent(contexto);
                            cfp.addReceiver(r);
                            send(cfp);
                        }
                    } 
                    else if (reply.getPerformative() == ACLMessage.PROPOSE) {
                        String[] partes = reply.getContent().split("\\|");
                        String cancionPropuesta = partes[0];
                        int scoreActual = Integer.parseInt(partes[1]);
                        respuestasRecibidas++;

                        System.out.println("Oyente: Recibida propuesta de [" + reply.getSender().getLocalName() + "] -> Canción: " + cancionPropuesta + " | Match: " + scoreActual + "%");

                        if (scoreActual > mejorScore) {
                            mejorScore = scoreActual;
                            mejorCancion = partes[0];
                        }

                        //respuestas recibidas de los 2 agentes recomendadores
                        if (respuestasRecibidas >= 2) {  
                            System.out.println(">>> GANADOR: " + mejorCancion + " (" + mejorScore + "%)");
                            
                            // buscar agente historial en páginas amarillas
                            AID historial = buscarServicioLocal("historial-musical");
                            if (historial != null) {
                                ACLMessage histMsg = new ACLMessage(ACLMessage.INFORM);
                                histMsg.addReceiver(historial);
                                histMsg.setContent(mejorCancion + "|" + mejorScore);
                                send(histMsg);
                            }
                            respuestasRecibidas = 0; mejorScore = -1;
                        }
                    }
                } else { block(); }
            }
        });
    }

    private AID buscarServicioLocal(String tipo) {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(tipo);
        template.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) return result[0].getName();
        } catch (FIPAException fe) { fe.printStackTrace(); }
        return null;
    }

    private AID[] buscarServiciosRemotos(String tipo) {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(tipo);
        template.addServices(sd);
        try {
            AID dfRemote = new AID("df@Plataforma2", AID.ISGUID);
            dfRemote.addAddresses("http://127.0.0.1:7779/acc");
            DFAgentDescription[] result = DFService.search(this, dfRemote, template);
            
            AID[] agentes = new AID[result.length];
            for (int i = 0; i < result.length; i++) {
                AID rAID = result[i].getName();
                rAID.clearAllAddresses(); 
                rAID.addAddresses("http://127.0.0.1:7779/acc");
                agentes[i] = rAID;
            }
            return agentes;
        } catch (FIPAException fe) { 
            System.err.println("Error buscando en DF remoto: " + fe.getMessage());
            return new AID[0]; 
        }
    }
}
