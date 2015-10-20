/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controller;

import dao.UsuarioJpaController;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import model.Usuario;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zul.Messagebox;

/**
 *
 * @author aluno
 */
public class HomeVM {
    private String login, senha;
    
    @Command
    @NotifyChange({"login","senha"})
    public void validaLogin(){
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("cadFornecedorPU");
        
        Usuario u = new UsuarioJpaController(emf).getUsuarioByLoginAndSenha(login, senha);
        
        if (u == null) {
            Messagebox.show("Login ou senha incorreto!");
            login = senha = "";
        } else {
            Sessions.getCurrent().setAttribute("user", u);
            Executions.getCurrent().sendRedirect("usuario.zul");
        }        
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }
    
    
}
