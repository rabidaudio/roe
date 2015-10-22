class SessionsController < Devise::SessionsController

  def new
    render json: {} # user should use post instead
  end

  def create
    self.resource = warden.authenticate!(auth_options)
    sign_in(resource_name, resource)
 
    current_user.update authentication_token: nil
    render json: current_user
  end

  def destroy
    if current_user
      current_user.update authentication_token: nil
      signed_out = (Devise.sign_out_all_scopes ? sign_out : sign_out(resource_name))
      render :json => {}.to_json, :status => :ok
    else
      render :json => {}.to_json, :status => :unprocessable_entity
    end
  end 

end