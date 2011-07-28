package br.pa.plus.genericservice;

import java.io.Serializable;
import java.sql.SQLException;

import javax.ejb.EJBException;
import javax.ejb.EJBTransactionRolledbackException;

import org.hibernate.exception.GenericJDBCException;
import org.hibernate.exception.SQLGrammarException;

import br.gov.pa.muiraquita.exception.BusinessException;
import br.gov.pa.muiraquita.exception.BusinessRuntimeException;
import br.gov.pa.muiraquita.exception.ErrorTrace;
import br.gov.pa.muiraquita.exception.PersistenceException;
import br.gov.pa.muiraquita.exception.SystemException;
import br.gov.pa.muiraquita.exception.business.NegocioException;
import br.gov.pa.muiraquita.exception.internal.InternalException;
import br.gov.pa.muiraquita.exception.retorno.CommonErrorReturn;
import br.gov.pa.muiraquita.exception.retorno.Erro;
import br.gov.pa.muiraquita.exception.retorno.Error;
import br.gov.pa.muiraquita.exception.retorno.Error.ErrorType;

public class ExceptionTreatment implements Serializable {


  private static final String ERRO_NAO_CONTROLADO = "Desculpe. Ocorreu uma falha no sistema.";

  private String COD_GEN = "0000";
  
  private Integer classType = 0; //0 is DTO; 1 is Entity
  
  private Class<?> classEntity;
  
  
  public void treat(Throwable throwable, Class<?> ucClass) throws BusinessException {
    
    classEntity = ucClass;
    
    treat(throwable);
  }
  

  @SuppressWarnings("deprecation")
  public void treat(Throwable throwable) throws BusinessException {
    
    CommonErrorReturn ret = new CommonErrorReturn();
    //Excessoes ok  
    if (throwable instanceof BusinessException) {
	      throw (BusinessException) throwable;
    } else if (throwable instanceof SystemException) {
      throw (SystemException) throwable;
    } else if (throwable instanceof InternalException) {
      throw (InternalException) throwable;
    } else if (throwable instanceof BusinessRuntimeException) {
      throw (BusinessRuntimeException) throwable;
    } else
    //Negocio Deprecateds
    if (throwable instanceof NegocioException) { 
    	NegocioException ne = (NegocioException) throwable;
      for (Erro erro : ne.getRetorno().getAttachments()) {
        ret.addAttachment(new Error(erro.getCodigo(), erro.getMensagem(), ErrorType.VALIDATION));
      }
      throw new BusinessException(ret);
    } 
  
    //Falhas de constraints do banco de dados
    else if (throwable instanceof javax.persistence.EntityExistsException) {
      javax.persistence.EntityExistsException ee = (javax.persistence.EntityExistsException) throwable;
      
      if(ee.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
        org.hibernate.exception.ConstraintViolationException cve = (org.hibernate.exception.ConstraintViolationException) ee.getCause();
        
        SQLException sqle = cve.getSQLException();
        
        ret.addAttachment(new Error("Contraint violation:" + cve.getConstraintName(), sqle.getLocalizedMessage(), ErrorType.PERSISTENCE));
        throw new BusinessException(ret);
        
      } else {
        ret.addAttachment(new Error(ee.getClass().getSimpleName(), ee.getMessage(), ErrorType.PERSISTENCE));
        throw new BusinessException(ret);
      }
    }
    
    else if (throwable instanceof javax.persistence.PersistenceException) {
      javax.persistence.PersistenceException ee = (javax.persistence.PersistenceException) throwable;
      
      if(ee.getCause() instanceof org.hibernate.exception.DataException) {
        org.hibernate.exception.DataException de = (org.hibernate.exception.DataException) ee.getCause();
        
        SQLException sqle = de.getSQLException();
        
        ret.addAttachment(new Error("Data Exception", sqle.getLocalizedMessage(), ErrorType.PERSISTENCE));
        throw new BusinessException(ret);
        
      } else {
        ret.addAttachment(new Error(ee.getClass().getSimpleName(), ee.getMessage(), ErrorType.PERSISTENCE));
        throw new BusinessException(ret);
      }
    }
    
    //Falhas com rollbakc feitas pelo ejb (eu acho!!)
    else if (throwable instanceof EJBException) {
      
      if (((EJBException) throwable).getCausedByException().getClass() == BusinessRuntimeException.class) {
        BusinessRuntimeException ne = (BusinessRuntimeException) ((EJBException) throwable).getCausedByException();
        ne.printStackTrace();
        throw ne;
      } else 
      if (((EJBException) throwable).getCausedByException().getClass() == NegocioException.class) {
        NegocioException ne = (NegocioException) ((EJBException) throwable).getCausedByException();
        ne.printStackTrace();
        for (Erro erro : ne.getRetorno().getAttachments()) {
          ret.addAttachment(new Error(erro.getCodigo(), erro.getMensagem(), ErrorType.VALIDATION));
        }
        throw new BusinessException(ret);
      } else
      if (throwable.getCause() instanceof PersistenceException) {

        PersistenceException ne = (PersistenceException) ((EJBException) throwable).getCausedByException();
        ne.printStackTrace();
      
        if (ne.getReturnError().getErrorMessages() != null) {
          if (ne.getReturnError().getErrorMessages().size() > 0) {
            
            for (String erro : ne.getReturnError().getErrorMessages()) {
              ret.addAttachment(new Error(COD_GEN, erro, ErrorType.VALIDATION));
            }
            throw new BusinessException(ret);
            
          } else {
            for (Error erro : ne.getReturnError().getAttachments()) {
              ret.addAttachment(new Error(erro.getCode(), erro.getMessage(), ErrorType.VALIDATION));
            }
            throw new BusinessException(ret);
          }
        } else {
          for (Error erro : ne.getReturnError().getAttachments()) {
              ret.addAttachment(new Error(erro.getCode(), erro.getMessage(), ErrorType.VALIDATION));
          }
        }
      } else
      if (((EJBException) throwable).getCausedByException().getClass() == GenericJDBCException.class) {
        GenericJDBCException ne = (GenericJDBCException) ((EJBTransactionRolledbackException) throwable).getCausedByException();
        ne.printStackTrace();
        
        ret.addAttachment(new Error("DataBaseError", "Ocorreu uma falha na conexão com o banco de dados", ErrorType.PERSISTENCE));
        throw new PersistenceException(ret);
        
      } else 
        if(throwable.getCause() instanceof BusinessRuntimeException){
        BusinessRuntimeException ne = (BusinessRuntimeException) ((EJBTransactionRolledbackException) throwable).getCausedByException();
          ErrorTrace.print(ne);
          
          if (ne.getReturnError().getErrorMessages() != null) {
            if (ne.getReturnError().getAttachments().size() > 0) {
               
              //addMessageError(ne.getReturnError().getAttachments());
              for (Error erro : ne.getReturnError().getAttachments()) {
                ret.addAttachment(new Error(erro.getCode(), erro.getMessage(), ErrorType.VALIDATION));
              }
              
            } else {
              for (Error erro : ne.getReturnError().getAttachments()) {
                ret.addAttachment(new Error(erro.getCode(), erro.getMessage(), ErrorType.VALIDATION));
              }
            }
          } else {
            for (Error erro : ne.getReturnError().getAttachments()) {
              ret.addAttachment(new Error(erro.getCode(), erro.getMessage(), ErrorType.VALIDATION));
            }
          }
          throw new BusinessRuntimeException(ret);
      }
      else {  
        //enviarEmailErroFatal(throwable);
        ret.addAttachment(new Error("FATAL ERROR", ERRO_NAO_CONTROLADO, ErrorType.INTERNAL));
        throwable.printStackTrace();
      }
    }
	    
	    //Falhas com rooback feito pelo container
	    else if (throwable instanceof EJBTransactionRolledbackException) {
	      
	      if (((EJBTransactionRolledbackException) throwable).getCausedByException().getClass() == BusinessRuntimeException.class) {
	        BusinessRuntimeException ne = (BusinessRuntimeException) ((EJBTransactionRolledbackException) throwable).getCausedByException();
          ne.printStackTrace();
          if (ne.getReturnError().getErrorMessages() != null) {
            if (ne.getReturnError().getErrorMessages().size() > 0) {
              for (Error erro : ne.getReturnError().getAttachments()) {
                ret.addAttachment(new Error(erro.getCode(), erro.getMessage(), ErrorType.VALIDATION));
              }
            } else {
              for (Error erro : ne.getReturnError().getAttachments()) {
                ret.addAttachment(new Error(erro.getCode(), erro.getMessage(), ErrorType.VALIDATION));
              }
            }
          } else {
            for (Error erro : ne.getReturnError().getAttachments()) {
              ret.addAttachment(new Error(erro.getCode(), erro.getMessage(), ErrorType.VALIDATION));
            }
          }
          throw new BusinessRuntimeException(ret);
        } else 
	      if (((EJBTransactionRolledbackException) throwable).getCausedByException().getClass() == NegocioException.class) {
	        NegocioException ne = (NegocioException) ((EJBTransactionRolledbackException) throwable).getCausedByException();
	        ne.printStackTrace();
	        if (ne.getRetorno().getErrorMessages() != null) {
	          if (ne.getRetorno().getErrorMessages().size() > 0) {
	            for (Erro erro : ne.getRetorno().obterAnexos()) {
                ret.addAttachment(new Error(erro.getCodigo(), erro.getMensagem(), ErrorType.VALIDATION));
              }
	          } else {
	            for (Erro erro : ne.getRetorno().obterAnexos()) {
	              ret.addAttachment(new Error(erro.getCodigo(), erro.getMensagem(), ErrorType.VALIDATION));
	            }
	          }
	        } else {
	          for (Erro erro : ne.getRetorno().obterAnexos()) {
	            ret.addAttachment(new Error(erro.getCodigo(), erro.getMensagem(), ErrorType.VALIDATION));
	          }
	        }
	        throw new BusinessException(ret);
	      } else
	      if (throwable.getCause() instanceof PersistenceException) {
	        if(throwable.getCause().getCause() instanceof SQLGrammarException) {
	          SQLGrammarException ne = (SQLGrammarException) throwable.getCause().getCause();
	          ne.printStackTrace();
	          ret.addAttachment(new Error(String.valueOf(ne.getErrorCode()), ne.getMessage(), ErrorType.PERSISTENCE));
	          throw new PersistenceException(ret);
	        } 
	        
	        //Falhas de JDBC puro
	        else	          
	        if(throwable.getCause().getCause() instanceof GenericJDBCException) {
	          GenericJDBCException ne = (GenericJDBCException) throwable.getCause().getCause();
	          System.out.println("GenericJDBCException : Falha na conexao com o banco");
	          ne.printStackTrace();
            ret.addAttachment(new Error("DataBase Error", ne.getMessage(), ErrorType.PERSISTENCE));
            throw new PersistenceException(ret);
	        } 
	        else {
	          //addFatalMessage(ERRO_NAO_CONTROLADO);
	          throwable.printStackTrace();
	          ret.addAttachment(new Error("Fatal Error", ERRO_NAO_CONTROLADO, ErrorType.INTERNAL));
            throw new PersistenceException(ret);
	        }
	      } else
	      if (((EJBTransactionRolledbackException) throwable).getCausedByException().getClass() == GenericJDBCException.class) {
	        GenericJDBCException ne = (GenericJDBCException) ((EJBTransactionRolledbackException) throwable).getCausedByException();
	        ne.printStackTrace();
	        ret.addAttachment(new Error("DataBase Error", ne.getMessage(), ErrorType.PERSISTENCE));
          throw new PersistenceException(ret);
	      }else 
	        if(throwable.getCause() instanceof BusinessRuntimeException){
	    	  BusinessRuntimeException ne = (BusinessRuntimeException) ((EJBTransactionRolledbackException) throwable).getCausedByException();
	          ErrorTrace.print(ne);
	          if (ne.getReturnError().getErrorMessages() != null) {
	            if (ne.getReturnError().getAttachments().size() > 0) {
	              for (Error erro : ne.getReturnError().getAttachments()) {
	                ret.addAttachment(new Error(erro.getCode(), erro.getMessage(), ErrorType.PERSISTENCE));
	              }
	            } else {
	              for (Error erro : ne.getReturnError().getAttachments()) {
	                ret.addAttachment(new Error(erro.getCode(), erro.getMessage(), ErrorType.PERSISTENCE));
	              }
	            }
	          } else {
	            for (Error erro : ne.getReturnError().getAttachments()) {
	              ret.addAttachment(new Error(erro.getCode(), erro.getMessage(), ErrorType.PERSISTENCE));
	            }
	          }
	      } else {  
	        //enviarEmailErroFatal(throwable);
	        throwable.printStackTrace();
	        ret.addAttachment(new Error("Fatal Error", ERRO_NAO_CONTROLADO, ErrorType.INTERNAL));
          throw new PersistenceException(ret);
	      }
	      
	      throw new PersistenceException(ret);
	      
	    }// fim if(throwable instanceof EJBTransactionRolledbackException)
    
    
      //Falha não controlada
	    else {  
	      throwable.printStackTrace();

	      //Redirect.instance().setViewId("/seguranca/erro/erro.xhtml");
	      //Redirect.instance().execute();

	      ret.addAttachment(new Error("Fatal Error", ERRO_NAO_CONTROLADO, ErrorType.INTERNAL));
        throw new PersistenceException(ret);
	    }
	  }
  
  
  
}
