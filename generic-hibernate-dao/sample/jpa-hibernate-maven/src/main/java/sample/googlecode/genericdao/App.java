package sample.googlecode.genericdao;

import java.util.HashSet;
import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.googlecode.genericdao.dao.jpa.GeneralDAO;
import com.googlecode.genericdao.search.Search;

import sample.googlecode.genericdao.model.Citizen;
import sample.googlecode.genericdao.model.Town;
import sample.googlecode.genericdao.service.CitizenService;
import sample.googlecode.genericdao.service.TownService;


/**
 * Hello world!
 *
 */
public class App 
{
	private static CitizenService citizenService;
	private static TownService townService;
	private static GeneralDAO generalDAO;
	
    public static void main( String[] args )
    {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext.xml");
    	
        citizenService = (CitizenService) ctx.getBean("citizenService");
        townService = (TownService) ctx.getBean("townService");
        generalDAO = (GeneralDAO) ctx.getBean("generalDAO");
        
    	initData();
    	
    	for (Town town : townService.findAll()) {
    		System.out.println(town.getName() + " (population " + town.getPopulation() + ")");
    		town = townService.findByName(town.getName());
    		for (Citizen citizen : town.getCitizens()) {
    			System.out.println(" - " + citizen.getName() + " is a " + citizen.getJob());
    		}
    	}
    	
    	System.out.println("Searching for citizens named Dick...");
    	
    	for (Citizen citizen : (List<Citizen>) generalDAO.search(new Search(Citizen.class).addFilterLike("name", "Dick%"))) {
    		System.out.println(" - " + citizen.getName() + " is a " + citizen.getJob());
    	}
    	
    	System.out.println("Done.");
    }
    
    private static void initData() {
    	Town nottingham = new Town();
    	nottingham.setName("Nottingham");
    	nottingham.setPopulation(126);
    	nottingham.setCitizens(new HashSet<Citizen>());
    	townService.persist(nottingham);
    	
    	Citizen butcher = new Citizen();
    	butcher.setJob("butcher");
    	butcher.setName("Tom Butcher");
    	butcher.setTown(nottingham);
    	nottingham.getCitizens().add(butcher);
    	
    	Citizen baker = new Citizen();
    	baker.setJob("baker");
    	baker.setName("Dick Baker");
    	baker.setTown(nottingham);
    	nottingham.getCitizens().add(baker);
    	
    	Citizen chandler = new Citizen();
    	chandler.setJob("candlestick maker");
    	chandler.setName("Harry Chandler");
    	chandler.setTown(nottingham);
    	nottingham.getCitizens().add(chandler);
    	
    	citizenService.persist(butcher);
    	citizenService.persist(baker);
    	citizenService.persist(chandler);
    }
}
