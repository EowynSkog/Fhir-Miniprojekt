import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.GregorianCalendar;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.ClinicalImpression;
import org.hl7.fhir.dstu3.model.ClinicalImpression.ClinicalImpressionStatus;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Condition;
import org.hl7.fhir.dstu3.model.Condition.ConditionClinicalStatus;
import org.hl7.fhir.dstu3.model.Condition.ConditionVerificationStatus;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.Encounter.EncounterHospitalizationComponent;
import org.hl7.fhir.dstu3.model.Encounter.EncounterStatus;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.EpisodeOfCare.EpisodeOfCareStatus;
import org.hl7.fhir.dstu3.model.EpisodeOfCare;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Location;
import org.hl7.fhir.dstu3.model.Narrative;
import org.hl7.fhir.dstu3.model.Narrative.NarrativeStatus;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.RiskAssessment;
import org.hl7.fhir.dstu3.model.RiskAssessment.RiskAssessmentStatus;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.dstu3.model.ReferralRequest.ReferralCategory;
import org.hl7.fhir.dstu3.model.ReferralRequest.ReferralPriority;
import org.hl7.fhir.dstu3.model.ReferralRequest.ReferralRequestRequesterComponent;
import org.hl7.fhir.dstu3.model.ReferralRequest.ReferralRequestStatus;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;

public class App {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//TODO die RandomUuids funktionieren nur wenn die Ressourcen gleichzeitig (im Bundle)auf den Server geladen werden. Muss noch implementiert werden und getestet werden, ob das wirklich funktioniert
		// connecting to a DSTU3 compliant server
		FhirContext ctx = FhirContext.forDstu3();

		String serverBase = "http://funke.imi.uni-luebeck.de/public/base/";
		// String serverBase = "http://fhirtest.uhn.ca/baseDstu3";

		IGenericClient client = ctx.newRestfulGenericClient(serverBase);

		// Create a patient object
		Patient patient = new Patient();
		// add an ID
		patient.addIdentifier().setSystem("http://www.kh-hh.de/mio/patients").setValue("PID0092342");
		// add patients gender
		patient.setGender(AdministrativeGender.MALE);
		// add patients name
		patient.addName().setUse(HumanName.NameUse.OFFICIAL).setFamily("Mythenmetz").addGiven("Hildegunst")
				.addPrefix("von");
		// add patients BirthDate
		patient.setBirthDate(new GregorianCalendar(1498, 7, 6).getTime());
		// add address
		patient.addAddress().setCity("Lindwurmfeste").setCountry("Zamonien").setPostalCode("94041")
				.addLine("4 Silbendrechslerstr.");
		// add patients marital status to married MaritalStatusCodes.M
		CodeableConcept matStat = new CodeableConcept();
		matStat.addCoding().setCode("D").setSystem("http://hl7.org/fhir/v3/MaritalStatus").setDisplay("Divorced");
		patient.setMaritalStatus(matStat);
		// Textuelle Zusammenfassung
		Narrative na = new Narrative();
		na.setDivAsString(
				"Patient Hildegunst von Mythenmetz, geboren am 6.7.1489, Familienstand: geschieden, Adresse: Silbendrechslerstr. 4, 94041 Lindwurmfeste, Zamonien");
		na.setStatus(NarrativeStatus.GENERATED);
		patient.setText(na);
		
		// Läd den neuen Patienten auf den Server (muss man nicht bei jedem Testdurchlauf machen ;) )
		MethodOutcome outcome = client.create()
	 			   .resource(patient)
	 			   .prettyPrint()
	 			   .encodedJson()
	 			   .execute();
			IdDt patientId = (IdDt) outcome.getId();

		
		IParser jsonParser = ctx.newJsonParser();
		jsonParser.setPrettyPrint(true);
		String encoded = jsonParser.encodeResourceToString(patient);
		System.out.println(encoded);

		// Retrieve Emma Emergency and Adam Careful as participant for the
		// encouter with the patient
		// Emma
		Bundle bundle = (Bundle) client.search().forResource(Practitioner.class).where(
				new TokenClientParam("identifier").exactly().systemAndCode("urn:oid:2.16.528.1.1007.3.1", "874635264"))
				.prettyPrint().execute();

		BundleEntryComponent entry = bundle.getEntry().get(0);
		Practitioner emma = (Practitioner) entry.getResource();

		// Adam
		bundle = (Bundle) client.search().forResource(Practitioner.class).where(
				new TokenClientParam("identifier").exactly().systemAndCode("urn:oid:2.16.528.1.1007.3.1", "8746123287"))
				.prettyPrint().execute();

		entry = bundle.getEntry().get(0);
		Practitioner adam = (Practitioner) entry.getResource();

		//MIO Krankenhaus
		bundle = (Bundle) client.search().forResource(Organization.class)
				.where(new TokenClientParam("_id").exactly().code("11907"))
				.prettyPrint()
				.execute();
		entry = bundle.getEntry().get(0);
		Organization khaus = (Organization) entry.getResource();
		
		//Intensievstation
		bundle = (Bundle) client.search().forResource(Location.class)
				.where(new StringClientParam("name").matches().value("UKSH ICU 1"))
				.prettyPrint()
				.execute();
		entry = bundle.getEntry().get(0);
		Location icu =(Location) entry.getResource();
		
		// Verdachtsdiagnose
		Condition vDiag = new Condition();
		vDiag.addIdentifier().setSystem("http://www.kh-hh.de/mio/Conditions").setValue("C0394");
		vDiag.setClinicalStatus(ConditionClinicalStatus.ACTIVE);
		vDiag.setVerificationStatus(ConditionVerificationStatus.PROVISIONAL);
		vDiag.addCategory().addCoding().setSystem("http://hl7.org/fhir/condition-category")
				.setCode("encounter-diagnosis").setDisplay("Encounter Diagnosis");
		vDiag.setCode(new CodeableConcept().addCoding(new Coding().setSystem("http://snomed.info/sct")
				.setCode("110030002").setDisplay("Concussion injury of brain")));
		// icd Diagnose
		vDiag.setCode(new CodeableConcept().addCoding(new Coding().setSystem("http://hl7.org/fhir/sid/icd-10")
				.setCode("S06.0").setDisplay("Gehirnerschütterung")));
		vDiag.setSubject(new Reference(patientId));
		vDiag.setAsserterTarget(emma);
		na = new Narrative();
		na.setDivAsString(
				" Notärztin Emma Emergency stellt dem Patienten Mythenmetz die Verdachtsdiagnose Gehirnerschütterung.");
		na.setStatus(NarrativeStatus.GENERATED);
		vDiag.setText(na);
		
		// Läd die Verdachtsdiagnose auf den Server (muss man nicht bei jedem Testdurchlauf machen ;) )
		outcome = client.create()
 			   .resource(vDiag)
 			   .prettyPrint()
 			   .encodedJson()
 			   .execute();
		IdDt vDiagId = (IdDt) outcome.getId();

		// Einweisung nach Verkehrsunfall
		ReferralRequest einweisung = new ReferralRequest();
		einweisung.addIdentifier().setSystem("http://www.kh-hh.de/mio/ReferralReq").setValue("EW0094");
		einweisung.setStatus(ReferralRequestStatus.ACTIVE);
		einweisung.setIntent(ReferralCategory.ORDER);
		einweisung.setType(new CodeableConcept().addCoding(new Coding().setSystem("http://snomed.info/sct")
				.setCode("183866009").setDisplay("Referral to emergency clinic")));
		einweisung.setPriority(ReferralPriority.ASAP);
		einweisung.addServiceRequested(new CodeableConcept().addCoding(new Coding().setSystem("http://snomed.info/sct")
				.setCode("707852009").setDisplay("Inpatient management required")));
		einweisung.setSubject(new Reference(patientId));
		einweisung.setRequester(new ReferralRequestRequesterComponent().setAgentTarget(emma));
		einweisung.addRecipient().setResource(adam);
		einweisung.addReasonCode().addCoding().setSystem("http://snomed.info/sct").setCode("127348004")
				.setDisplay("Motor vehicle accident victim");
		einweisung.addReasonCode().addCoding().setSystem("http://snomed.info/sct").setCode("110030002")
				.setDisplay("Concussion injury of brain"); 															
		// Verdachtsdiagnose
		einweisung.addReasonReference().setResource(vDiag);
		na = new Narrative();
		na.setDivAsString(
				"Notärztin Emma Emergency weist Patienten Mythenmetz nach einem Verkehrsunfall in die Notaufname ein. Der Patient bekommt die Verdachtsdiagnose Gehirnerschütterung.");
		na.setStatus(NarrativeStatus.GENERATED);
		einweisung.setText(na);
		
		// Läd die Einweisung auf den Server (muss man nicht bei jedem Testdurchlauf machen ;) )
		outcome = client.create()
	 			   .resource(einweisung)
	 			   .prettyPrint()
	 			   .encodedJson()
	 			   .execute();
			IdDt einweisungId = (IdDt) outcome.getId();
					
		
		//EpisodeOfCare (=Zeitraum in dem das Mio Krankenhaus für die med. Versorgung des Patienten Mytenmetz verantwortlich ist) startet. Diese EpisodeOfCare verbindet alle zugehörigen Encounter
		EpisodeOfCare eoc = new EpisodeOfCare();
		eoc.setId(IdDt.newRandomUuid());
		eoc.addIdentifier().setSystem("http://www.kh-hh.de/mio/EpisodesOfCare").setValue("EC009319");
		eoc.setStatus(EpisodeOfCareStatus.ACTIVE);
		eoc.addDiagnosis().setConditionTarget(vDiag).setRole(new CodeableConcept().addCoding(new Coding().setSystem("http://hl7.org/fhir/diagnosis-role").setCode("AD").setDisplay("Admission diagnosis")));
		eoc.setPatient(new Reference(patientId));
		eoc.setManagingOrganizationTarget(khaus);
		eoc.setPeriod(new Period().setStart(new GregorianCalendar(2017,10,13).getTime()));
		eoc.addReferralRequest(new Reference(einweisungId));
		na = new Narrative();
		na.setDivAsString("Zeitraum in dem das MIO Krankenhaus für die med. Versorgung des Patienten Mythenmetz verantwortlich ist beginnt am 13.10.2017. Grund dieses Krankhenhausaufenthalts ist die  Abklärung der Verdachtsdiagnose Gehirnerschütterung" );
		na.setStatus(NarrativeStatus.GENERATED);
		eoc.setText(na);
		
		// Läd die EoC auf den Server (muss man nicht bei jedem Testdurchlauf machen ;) )
		outcome = client.create()
	 			   .resource(eoc)
	 			   .prettyPrint()
	 			   .encodedJson()
	 			   .execute();
			IdDt eocId = (IdDt) outcome.getId();
		
		
		
		// eingetrübter Bewusstseinszustand
		Condition condition = new Condition();
		condition.setId(IdDt.newRandomUuid());
		condition.addIdentifier().setSystem("http://www.kh-hh.de/mio/Conditions").setValue("C0397");
		condition.setClinicalStatus(ConditionClinicalStatus.ACTIVE);
		condition.setVerificationStatus(ConditionVerificationStatus.DIFFERENTIAL);
		condition.addCategory().addCoding().setSystem("http://hl7.org/fhir/condition-category")
				.setCode("encounter-diagnosis").setDisplay("Encounter Diagnosis");
		condition.setSeverity(new CodeableConcept().addCoding(new Coding().setSystem("http://snomed.info/sct")
				.setCode("24484000").setDisplay("Severe")));
		condition.setCode(new CodeableConcept().addCoding(new Coding().setSystem("http://snomed.info/sct")
				.setCode("40917007").setDisplay("Clouded consciousness")));
		condition.setSubjectTarget(patient);
		condition.setAsserterTarget(adam);
		na = new Narrative();
		na.setDivAsString(
				"Arzt Adam Careful stellt einen eingetrübten Bewusstseinszustand beim Patient Mythenmetz fest.");
		na.setStatus(NarrativeStatus.GENERATED);
		condition.setText(na);
		
		// Läd die Condition auf den Server (muss man nicht bei jedem Testdurchlauf machen ;) )
		outcome = client.create()
	 			   .resource(condition)
	 			   .prettyPrint()
	 			   .encodedJson()
	 			   .execute();
			IdDt conditionId = (IdDt) outcome.getId();
		
		
		Encounter aufnahme = new Encounter();
		aufnahme.addIdentifier().setSystem("http://www.kh-hh.de/mio/Encounter").setValue("EN04840");
		aufnahme.setStatus(EncounterStatus.ARRIVED);
		aufnahme.setClass_(new Coding().setSystem("http://hl7.org/fhir/v3/ActCode").setCode("ACUTE")
				.setDisplay("inpatient acute"));
		aufnahme.setPriority(new CodeableConcept().addCoding(
				new Coding().setSystem("http://hl7.org/fhir/v3/ActPriority").setCode("EL").setDisplay("elective")));
		aufnahme.setSubject(new Reference(patientId));
		aufnahme.addEpisodeOfCare(new Reference(eocId));
		aufnahme.addIncomingReferral(new Reference(einweisungId));
		aufnahme.setPeriod(new Period().setStart(new GregorianCalendar(2017, 9, 1).getTime()));
		aufnahme.setServiceProvider(new Reference(khaus.getId()));
		aufnahme.addReason().addCoding().setCode("407153006").setSystem("http://snomed.info/sct")
				.setDisplay("Motor vehicle injury (disorder)");
		aufnahme.addParticipant()
				.addType(new CodeableConcept().addCoding(new Coding().setCode("ADM").setDisplay("admitter")
						.setSystem("http://hl7.org/fhir/v3/ParticipationType")))
				.setIndividual(new Reference((adam.getId())));
		
		aufnahme.addDiagnosis().setConditionTarget(vDiag).setRole(new CodeableConcept().addCoding(new Coding().setSystem("http://hl7.org/fhir/diagnosis-role").setCode("AD").setDisplay("Admission diagnosis")));
		aufnahme.addDiagnosis().setConditionTarget(condition).setRole(new CodeableConcept().addCoding(new Coding().setSystem("http://hl7.org/fhir/diagnosis-role").setCode("CM").setDisplay("Comorbidity diagnosis")));
		aufnahme.setHospitalization(new EncounterHospitalizationComponent()
				.setAdmitSource(new CodeableConcept().addCoding(new Coding().setSystem("http://hl7.org/fhir/admit-source").setCode("emd").setDisplay("From accident/emergency department")))
				.setDestinationTarget(icu));
		
		na = new Narrative();
		na.setDivAsString(
				"Wegen des eingetrübten Bewusstseinszustands entscheidet der aufnehmende Arzt Adam Careful die direkte Aufnahme auf die chirurgische Intensivstation UKSH ICU 1");
		na.setStatus(NarrativeStatus.GENERATED);
		aufnahme.setText(na);
		
		// Läd die aufnahme auf den Server (muss man nicht bei jedem Testdurchlauf machen ;) )
				outcome = client.create()
			 			   .resource(aufnahme)
			 			   .prettyPrint()
			 			   .encodedJson()
			 			   .execute();
					IdDt aufnahmeId = (IdDt) outcome.getId();
		
		
		//Der Arzt Careful stuft den Gesundheitszustand des Patienten Mythenmetz auf "akut bedroht" ein. (Aufgurnd der Verdachtsdiagnose im Zusammenhang mit dem beobachteten eingetrübten Bewusstseinszustand) 
// richtige (?!) Lösung über ClinicalImpression siehe unten
//		RiskAssessment riskLevel = new RiskAssessment();
//		riskLevel.setIdentifier(new Identifier().setSystem("http://www.kh-hh.de/mio/RiskLevel").setValue("RL04840"));
//		riskLevel.setStatus(RiskAssessmentStatus.REGISTERED);
//		riskLevel.setSubject(new Reference(patientId));
//		riskLevel.setContext(new Reference(eocId));
//		riskLevel.setOccurrence(new Period().setStart(new GregorianCalendar(2017,10,13).getTime()));
//		riskLevel.setCondition(new Reference(conditionId));
//		riskLevel.setPerformer(new Reference(adam.getId()));
//		riskLevel.addPrediction().setOutcome(new CodeableConcept().addCoding(
//				new Coding().setSystem("http://snomed.info/sct").setCode("18131002").setDisplay("Acute fulminating")));
//		riskLevel.setMitigation("Monitoring auf der Intensivstation");
//		na = new Narrative();
//		na.setDivAsString("Einstufung des Patienten Mythenmetz als akut gefähdet, durch den Arzt Careful");
//		na.setStatus(NarrativeStatus.GENERATED);
		
		ClinicalImpression cImp = new ClinicalImpression();
		cImp.addIdentifier().setSystem("http://www.kh-hh.de/mio/ClinImp").setValue("CI04840");
		cImp.setStatus(ClinicalImpressionStatus.DRAFT);
		cImp.setCode(new CodeableConcept().addCoding(
				new Coding().setSystem("http://snomed.info/sct").setCode("18131002").setDisplay("Acute fulminating")));
		cImp.setSubject(new Reference(patientId));
		cImp.setContext(new Reference(eocId));
		cImp.setContext(new Reference(aufnahmeId));
		cImp.setEffective(new Period().setStart(new GregorianCalendar(2017,10,13).getTime()));
		cImp.setDate(new GregorianCalendar(2017,10,13).getTime());
		cImp.setAssessorTarget(adam);
		cImp.addProblem(new Reference(conditionId));
		cImp.setSummary("Zustand des Patienten ist akut bedroht, da der Bewusstseinszustand eingetrübt ist");
		
		
		// Läd Clinische Beurteilung auf den Server
		//bitte nicht bei jedem Testdurchlauf den Server zumüllen
		outcome = client.create()
		   .resource(cImp)
		   .prettyPrint()
		   .encodedJson()
		   .execute();
	IdDt cImpId = (IdDt) outcome.getId();
		
		//Einweisung ist nun Abgeschlossen. Der Status muss entsprechend auf dem Server aktualisiert werden
		einweisung.setStatus(ReferralRequestStatus.COMPLETED);
				
		einweisung.setId(einweisungId);
		//bitte nicht bei jedem Testdurchlauf den Server zumüllen
//		MethodOutcome outcome = client.update().resource(einweisung).execute();
		
		

		// TODO Entlassung hier ist noch viel Blödsinn drin 
		// snomed 707851002 | Inpatient management not required (finding) |
		Encounter entlassung = new Encounter();
		entlassung.addIdentifier().setSystem("http://www.kh-hh.de/mio/Encounter").setValue("En40308");
		entlassung.setStatus(EncounterStatus.FINISHED);
		entlassung.setClass_(
				new Coding().setSystem("http://hl7.org/fhir/v3/ActCode").setCode("AMB").setDisplay("ambulatory"));
		entlassung.addType().addCoding(
				new Coding().setSystem("http://snomed.info/sct").setCode(" Outpatient procedure (procedure)"));
		entlassung.setPriority(new CodeableConcept().addCoding(
				new Coding().setSystem("http://hl7.org/fhir/v3/ActPriority").setCode("R").setDisplay("routine")));
		entlassung.setSubjectTarget(patient);

		IParser xmlParser = ctx.newXmlParser();
		xmlParser.setPrettyPrint(true);
		xmlParser.setSuppressNarratives(false);
		Writer fw = null;

		try {
			fw = new FileWriter("data/Aufnahme.xml");
			fw.write(xmlParser.encodeResourceToString(aufnahme));
			fw.write(System.getProperty("line.separator"));
			System.out.println("Datei Aufnahme erstellt");

		} catch (IOException e) {
			System.err.println("Konnte Datei nicht erstellen");
		} finally {
			if (fw != null)
				try {
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

	}
	
	  private static void createResource(IGenericClient client, Resource res)
	    {
	    	MethodOutcome outcome = client.create()
	    			   .resource(res)
	    			   .prettyPrint()
	    			   .encodedJson()
	    			   .execute();
	    	
	    	System.out.println("Got ID of created Resource: " + outcome.getId());
	    	
	    }
	 

}
