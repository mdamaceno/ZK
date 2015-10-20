/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import dao.exceptions.IllegalOrphanException;
import dao.exceptions.NonexistentEntityException;
import dao.exceptions.PreexistingEntityException;
import java.io.Serializable;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import model.Cidade;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import model.Estado;

/**
 *
 * @author aluno
 */
public class EstadoJpaController implements Serializable {

    public EstadoJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Estado estado) throws PreexistingEntityException, Exception {
        if (estado.getCidadeList() == null) {
            estado.setCidadeList(new ArrayList<Cidade>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            List<Cidade> attachedCidadeList = new ArrayList<Cidade>();
            for (Cidade cidadeListCidadeToAttach : estado.getCidadeList()) {
                cidadeListCidadeToAttach = em.getReference(cidadeListCidadeToAttach.getClass(), cidadeListCidadeToAttach.getId());
                attachedCidadeList.add(cidadeListCidadeToAttach);
            }
            estado.setCidadeList(attachedCidadeList);
            em.persist(estado);
            for (Cidade cidadeListCidade : estado.getCidadeList()) {
                Estado oldEstadoIdOfCidadeListCidade = cidadeListCidade.getEstadoId();
                cidadeListCidade.setEstadoId(estado);
                cidadeListCidade = em.merge(cidadeListCidade);
                if (oldEstadoIdOfCidadeListCidade != null) {
                    oldEstadoIdOfCidadeListCidade.getCidadeList().remove(cidadeListCidade);
                    oldEstadoIdOfCidadeListCidade = em.merge(oldEstadoIdOfCidadeListCidade);
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            if (findEstado(estado.getId()) != null) {
                throw new PreexistingEntityException("Estado " + estado + " already exists.", ex);
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Estado estado) throws IllegalOrphanException, NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Estado persistentEstado = em.find(Estado.class, estado.getId());
            List<Cidade> cidadeListOld = persistentEstado.getCidadeList();
            List<Cidade> cidadeListNew = estado.getCidadeList();
            List<String> illegalOrphanMessages = null;
            for (Cidade cidadeListOldCidade : cidadeListOld) {
                if (!cidadeListNew.contains(cidadeListOldCidade)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain Cidade " + cidadeListOldCidade + " since its estadoId field is not nullable.");
                }
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            List<Cidade> attachedCidadeListNew = new ArrayList<Cidade>();
            for (Cidade cidadeListNewCidadeToAttach : cidadeListNew) {
                cidadeListNewCidadeToAttach = em.getReference(cidadeListNewCidadeToAttach.getClass(), cidadeListNewCidadeToAttach.getId());
                attachedCidadeListNew.add(cidadeListNewCidadeToAttach);
            }
            cidadeListNew = attachedCidadeListNew;
            estado.setCidadeList(cidadeListNew);
            estado = em.merge(estado);
            for (Cidade cidadeListNewCidade : cidadeListNew) {
                if (!cidadeListOld.contains(cidadeListNewCidade)) {
                    Estado oldEstadoIdOfCidadeListNewCidade = cidadeListNewCidade.getEstadoId();
                    cidadeListNewCidade.setEstadoId(estado);
                    cidadeListNewCidade = em.merge(cidadeListNewCidade);
                    if (oldEstadoIdOfCidadeListNewCidade != null && !oldEstadoIdOfCidadeListNewCidade.equals(estado)) {
                        oldEstadoIdOfCidadeListNewCidade.getCidadeList().remove(cidadeListNewCidade);
                        oldEstadoIdOfCidadeListNewCidade = em.merge(oldEstadoIdOfCidadeListNewCidade);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = estado.getId();
                if (findEstado(id) == null) {
                    throw new NonexistentEntityException("The estado with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void destroy(Integer id) throws IllegalOrphanException, NonexistentEntityException {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Estado estado;
            try {
                estado = em.getReference(Estado.class, id);
                estado.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The estado with id " + id + " no longer exists.", enfe);
            }
            List<String> illegalOrphanMessages = null;
            List<Cidade> cidadeListOrphanCheck = estado.getCidadeList();
            for (Cidade cidadeListOrphanCheckCidade : cidadeListOrphanCheck) {
                if (illegalOrphanMessages == null) {
                    illegalOrphanMessages = new ArrayList<String>();
                }
                illegalOrphanMessages.add("This Estado (" + estado + ") cannot be destroyed since the Cidade " + cidadeListOrphanCheckCidade + " in its cidadeList field has a non-nullable estadoId field.");
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            em.remove(estado);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Estado> findEstadoEntities() {
        return findEstadoEntities(true, -1, -1);
    }

    public List<Estado> findEstadoEntities(int maxResults, int firstResult) {
        return findEstadoEntities(false, maxResults, firstResult);
    }

    private List<Estado> findEstadoEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Estado.class));
            Query q = em.createQuery(cq);
            if (!all) {
                q.setMaxResults(maxResults);
                q.setFirstResult(firstResult);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public Estado findEstado(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Estado.class, id);
        } finally {
            em.close();
        }
    }

    public int getEstadoCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Estado> rt = cq.from(Estado.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
