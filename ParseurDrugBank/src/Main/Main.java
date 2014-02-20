package Main;


import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.ParseException;
import org.jdom2.DataConversionException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

import Serveur.SingletonServeur;
import plbc.SimpleLuceneSearch;

public class Main {

	static Document document;
	static Element racine;

	public static void main(String[] args) throws IOException, ParseException, ClassNotFoundException, SQLException
	{

		SAXBuilder sxb = new SAXBuilder();
		try
		{
			document = sxb.build(new File("drugbank.xml"));
		}
		catch(Exception e){}

		racine = document.getRootElement();
		genereDrugList();
		System.out.println("------ FIN -------");
	}
	
	static void genereDrugList() throws IOException, ParseException, ClassNotFoundException, SQLException{
		
		//appel objet pour recuperer l'id d'un medicament
		SimpleLuceneSearch searchInMrConso = null;
		try {
			searchInMrConso = new SimpleLuceneSearch("indexOnMesh2012");
			
		} catch (CorruptIndexException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		PrintWriter out = new PrintWriter("nameDrug.ttl");
		//ecrit les prefix
		out.println("@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .");
		out.println("@prefix dr: <http://telecomnancy.eu/drug/> .");
		out.println("@prefix ge: <http://telecomnancy.eu/gene/> .");
		out.println("@prefix db: <http://telecomnancy.eu/db/> .");
		
		double nbNomNonTrouve = 0;
		double nbNom = 0;
		int nbFils = racine.getChildren().size();
		for(int i=0;i<nbFils;i++){
			Element drug = racine.getChildren().get(i);
			if(drug.getName().equals("drug")){
				int nbFilsDrug = drug.getChildren().size();
				for(int j=0;j<nbFilsDrug;j++){
					Element nameDrug = drug.getChildren().get(j);
					//id global du medicament etudiee
					String idGlobalMedicament ="";
					//balise name
					if(nameDrug.getName().equals("name")){
						nbNom++;
						String drugCui=searchInMrConso.getCuidFromLabel(nameDrug.getText());
						if(!drugCui.equals("")){
							idGlobalMedicament =  drugCui;
							out.println("dr:"+drugCui+" rdfs:hasLabel \""+nameDrug.getText()+"\" .");
						}else{
							nbNomNonTrouve++;
						}
					}
					for(Element roleDrug : drug.getChildren()){
					if(!idGlobalMedicament.equals("")){// si on a trouve de link au nom dans CUI
						//balise target
						if(roleDrug.getName().equals("targets")){
							
							List<Element> targetsDrug = roleDrug.getChildren();//dans la balise targets
							for(Element targetDrug : targetsDrug){
								//actions
								Element actionsDrug = targetDrug.getChildren().get(0);
								//action
								List<Element> listActionDrug = actionsDrug.getChildren();
								for(Element actionDrug : listActionDrug){
									String various = actionDrug.getText();
									String idPartner = targetDrug.getAttributeValue("partner");
									//print dans le fichier
									String geneId = findGeneID(idPartner);
									if(geneId.length() != 0)//TODO verifier cote ou pas autour de geneId
									out.println("dr:"+idGlobalMedicament+" db:"+various+"OfTarget ge:"+geneId+" .");
								}
							}
						}
						//balise transporteur
						if(roleDrug.getName().equals("transporters")){
							List<Element> targetsDrug = roleDrug.getChildren();//dans la balise targets
							for(Element targetDrug : targetsDrug){
								//actions
								Element actionsDrug = targetDrug.getChildren().get(0);
								//action
								List<Element> listActionDrug = actionsDrug.getChildren();
								for(Element actionDrug : listActionDrug){
									String various = actionDrug.getText();
									String idPartner = targetDrug.getAttributeValue("partner");
									//print dans le fichier
									String geneId = findGeneID(idPartner);
									if(geneId.length() != 0)
									out.println("dr:"+idGlobalMedicament+" db:"+various+"OfTransporter ge:"+geneId+" .");
								}
							}
						}
						//balise enzyme
						if(roleDrug.getName().equals("enzymes")){
							List<Element> targetsDrug = roleDrug.getChildren();//dans la balise targets
							for(Element targetDrug : targetsDrug){
								//actions
								Element actionsDrug = targetDrug.getChildren().get(0);
								//action
								List<Element> listActionDrug = actionsDrug.getChildren();
								for(Element actionDrug : listActionDrug){
									String various = actionDrug.getText();
									String idPartner = targetDrug.getAttributeValue("partner");
									//print dans le fichier
									String geneId = findGeneID(idPartner);
									if(geneId.length() != 0)
									out.println("dr:"+idGlobalMedicament+" db:"+various+"OfEnzyme ge:"+geneId+" .");
								}
							}
						}
					}
					}
				}
			}
		}
		out.close();
		System.out.println("nbNom : "+nbNom+", nbNomNonTrouve : "+nbNomNonTrouve);
		double proba =(nbNom-nbNomNonTrouve)/nbNom*100;
		System.out.println("Probabilite de nom trouve = " +proba);
	}
	
	static String findGeneID(String idPartner) throws ClassNotFoundException, SQLException{
		
		int nbFils = racine.getChildren().size();
		String geneIdName = "";
		for(int i=0;i<nbFils;i++){
			Element partners = racine.getChildren().get(i);
			if (partners.getName().equals("partners")) { 
				for(Element partner : partners.getChildren()){
					if(partner.getName().equals("partner")){
						try {
							if(partner.getAttribute("id").getIntValue() == Integer.parseInt(idPartner)){
								List<Element> geneNames = partner.getChildren();
								for(Element geneName : geneNames){
									if(geneName.getName().equals("gene-name")){
										geneIdName =geneName.getText();
									}
								}
							}
						} catch (NumberFormatException
								| DataConversionException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}

		}

		return SingletonServeur.getInstance().makeRequest(geneIdName);
	}

}
