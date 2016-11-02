package com.thejavageek.jaxrs;



import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuer;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.joda.time.DateTime;

import com.google.gson.Gson;

@Path("/HelloWorld")
public class HelloWorldService {

	//TESTING  DOWNLOAD UTENTI DA DB  E cavallo di troia

	@GET
	@Path("/getusers")
	@Produces (MediaType.APPLICATION_JSON)
	public Response  getUser(){

		EntityManagerFactory  emf = entityManagerUtils.getInstance();
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		List<User> result = em.createQuery( "from User", User.class ).getResultList();	
		em.getTransaction().commit();
		em.close();
		String resultJ = new Gson().toJson(result);
		return Response.status(200).entity(resultJ).build();
	}

	/////  ***********    SUM ONE DAY TO  CREATION DATE ************* 

	private String getDataExpired(){
		Date dt = new Date();
		DateTime dtOrg = new DateTime(dt);
		DateTime dtPlusOne = dtOrg.plusDays(1);
		String dataExpired = dtPlusOne.toString("yyyy.MM.dd.HH.mm.ss");
		return dataExpired;
	}
	
	
	
	///// ************ 	  NEW USER		***************	
	@POST
	@Path("/newUser")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public ResponseRequest registerUser(User u){
		ResponseRequest ResponseQuery = new ResponseRequest(); 
		EntityManagerFactory  emf = entityManagerUtils.getInstance();
		EntityManager em = emf.createEntityManager();
		if ((u.getUsername().length()<2) || (u.getPassword().length()<2) || (u.getEmail().length()<2) ){
			ResponseQuery.setSuccess(false);
			ResponseQuery.setCode(400);
			ResponseQuery.setDescription("Uno o pi� campi vuoti");
			return ResponseQuery;
		}
		em.getTransaction().begin();
		List<User> result = em.createQuery( " from User", User.class ).getResultList();
		for (int i=0;i<result.size();i++){

			if (result.get(i).getUsername().equals(u.getUsername()) || result.get(i).getEmail().equals(u.getEmail())){
				ResponseQuery.setSuccess(false);
				ResponseQuery.setCode(400);
				ResponseQuery.setDescription("Utente gi� registrato");
				em.getTransaction().commit();
				em.close();	
				return ResponseQuery;
			}
		}
		u.setPassword(convertMD5.encrypt(u.getPassword()));
		em.persist(u);	
		em.getTransaction().commit();
		em.close();
		ResponseQuery.setSuccess(true);
		ResponseQuery.setCode(200);
		ResponseQuery.setDescription("Utente registrato");
		return ResponseQuery;
	}

	@POST
	@Path("/login")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response login(User u) throws OAuthSystemException {
		//initialization variables
		ResponseRequest ResponseQuery = new ResponseRequest();
		token tokenResponse = new token();
		boolean find = false;
		u.setPassword(convertMD5.encrypt(u.getPassword()));
		EntityManagerFactory  emf = entityManagerUtils.getInstance();
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		//start

		List<User> result = em.createQuery( " from User", User.class ).getResultList();
		for (int i=0;i<result.size();i++){

			if (result.get(i).getUsername().equals(u.getUsername()) && result.get(i).getPassword().equals(u.getPassword())){
				tokenResponse = searchToken(result.get(i).getId());
				//if (tokenResponse.getAssignedToken().length()!=0){
				if(tokenResponse!=null){
					em.getTransaction().commit();
					em.close();
					return Response.status(200).entity(tokenResponse).build();	
				}
				else{	
				find=true;		/// user found in table users
				tokenResponse  = new token();
				tokenResponse.setIdUser(result.get(i).getId());
				tokenResponse.setDateCreate(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));
				tokenResponse.setDateExpired(getDataExpired());
				}
			} 	
		}
		if(find==true){

			OAuthIssuer oauthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());
			final String accessToken = oauthIssuerImpl.accessToken();
			tokenResponse.setAssignedToken(accessToken);
			em.persist(tokenResponse);	
			em.getTransaction().commit();
			em.close();

			return Response.status(200).entity(tokenResponse).build();		
		}else{
			ResponseQuery.setSuccess(false);
			ResponseQuery.setCode(404);
			ResponseQuery.setDescription("Utente non trovato");
			return Response.status(200).entity(ResponseQuery).build();	
		}
	}

	private token searchToken(int id) {
		token outputToken  = new token();
		EntityManagerFactory  emf = entityManagerUtils.getInstance();
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		//List<token> result = em.createQuery( " from user_token", token.class ).getResultList();
		outputToken = em.find(token.class, id);
		em.getTransaction().commit();
		em.close();
		return outputToken;
	}


}