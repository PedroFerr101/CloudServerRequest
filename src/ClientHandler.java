import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.List;

public class ClientHandler implements Runnable{
    private Socket cs;
    private PrintWriter out;
    private BufferedReader in;
    private Map<String, Utilizador> clients;
	private ServerManagement servers_typ1;

    private String active_user; // Coloquei isto porque quem trata do utilizador tem que saber qual ele é e isso só acontece apos o login


    public ClientHandler(Socket cs, Map<String, Utilizador> clients,ServerManagement s1) {
        this.cs = cs;
        this.clients = clients;
		this.servers_typ1 = s1;
    }

    public void run(){
        String msg;
        int check;

        try{
            out = new PrintWriter(cs.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(cs.getInputStream()));

            out.println("Bem-Vindo!");
            // Cliente deve fazer LogIn (e registar-se se necessário)
            check = validateAccess();


            if(check == 1) {
                int menu = 0;
				System.out.println("Login feito como: " + active_user);
				this.after_authentication();
            }

			System.out.println("Disconnected -> " + active_user); // no cliente as threads continuam a correr.
            in.close();
            out.close();
            cs.close();

        } catch (IOException e){
            System.out.println(e.getMessage());
        }
    }

    /*
    returns
        1  se autenticação é validada
        -1 se cliente cancela operação de log in ou registo
     */
    private int validateAccess() throws IOException {
        String msg;
        int opt=-1, check=-1;


        do{
            out.println("1 - Log In | 2 - Registar | 0 - Sair"); // tirei \n porque conta como linha nova. so se puser em ciclo a ler cada linha?
            msg = in.readLine();
            try{
                opt = Integer.parseInt(msg);
                String user;
                String pw; // declarei estes dois aqui porque dentro do switch nao dava idkw

                switch(opt) {
                    case 1:
                        do{
                            out.println("User: ");
                            user = in.readLine();
                            out.println("Password: ");
                            pw = in.readLine();

                            check = logIn(user, pw);
                            if(check == 1) return 1;
                        } while(check != 1);

                        break;
                    case 2:
                        do{
                            if(check == 0) out.println("Utilizador já existente.");
                            out.println("User: ");
                            user = in.readLine();
                            out.println("Password: ");
                            pw = in.readLine();

                            check = registerClient(user, pw);
                        } while(check != 1);

                        break;
                    case 0:
                        break;
                    default: out.println("Insira um dígito válido.\n");
                }

            }
            catch (NumberFormatException e){
                out.println("Input inválido. Insira um dígito.\n");
            }


        }while(opt != 0);

        return -1; // cliente cancela autenticação


    }




    // conforme a mensagem recebida fazer coisas (?)
    private void process(String msg) {
        System.out.println("Login feito como: " + active_user);
    }

	private void after_authentication() throws IOException {
		showOps(0);
		int opt = -1, v_leitura = -1;
		String msg;


		while(opt == -1){
			try{
				msg = in.readLine();
				v_leitura = Integer.parseInt(msg);

				switch(v_leitura){
					case 1: {
						this.adquirirServer();
						break;
					}

					case 2: {
						this.libertar();
						break;
					}

					case 3: {
						out.println("Valor da dívida -> " + this.divida_value(active_user));
						break;
					}

					case 4:{
						break;
					}

					default:{
						out.println("Insira um dígito válido.\n");
						v_leitura = -1;
						break;
					}
				}
			}
			catch (NumberFormatException e){
                out.println("Input inválido. Insira um dígito.\n");
            }

			opt = v_leitura;
		}

		if (opt != 4){
			this.after_authentication();
		}
	}

// é preciso lock no log in? lock no user?
    private int logIn(String user, String pw){
		Utilizador util;
        synchronized (clients){
            if(this.clients.containsKey(user) && this.clients.get(user).authenticate(pw)){
                this.active_user = user;
				return 1;
			}
        }
		out.println("Credenciais não batem certo.");
        return 0;
    }

    private int registerClient(String user, String pw) {
        synchronized (clients){
            if(!this.clients.containsKey(user)){
                this.clients.put(user,new Utilizador(user,pw));
                return 1;
            }
        }
        return 0;
    }

    void showOps(int menu){
        switch (menu){
            case 0:
                out.println("1 - Reservar servidor | 2 - Libertar servidor | 3 - Consultar divida | 4 - Sair");
        }
    }

	private Utilizador getUser(String str){
		Utilizador user;
		synchronized (clients){
			user = clients.get(str);
		}
		return user;
	}

	private void adquirirServer(){
		Utilizador user = this.getUser(this.active_user);
		String msg = servers_typ1.adquirir(10);
		user.addServidor(msg); // tem lock dentro da class
		out.println("Sou o rei comprei este fdp: " + msg);
	}

	private void libertar(){
		Utilizador user = this.getUser(this.active_user);
		try{
			String msg = in.readLine();

			if (user.donoServidor(msg)){
				double price = servers_typ1.libertar(msg);
				user.removeServidor(msg);
				user.addDivida(price);
				out.println("Libertei por: " + price);
			}
			else {
				out.println("Não és dono do server: " + msg);
			}
		}
		catch(IOException e){}
	}

	private double divida_value(String user){
		Utilizador util;
		double value = -1;

		synchronized (clients) {
			util = clients.get(user);
		}

		if (util != null){
			value = util.getDivida();
		}

		return value;
	}
}
