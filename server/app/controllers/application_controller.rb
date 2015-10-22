class ApplicationController < ActionController::API
  include ActionController::Serialization
  include ActionController::ImplicitRender
  
  rescue_from ActiveRecord::RecordNotFound, with: :record_not_found

  # before_action :authenticate_user

  def none
    render plain: "", status: :ok
  end
 
  private
  def authenticate_user
    # render json: Hash.new, status: 401 unless user_signed_in?
    render json: {error:{code: :unauthorized, message: "Please sign in"}}, status: :unauthorized unless user_signed_in?
  end

  def record_not_found
    render json: {error: {code: :not_found, message: "No such item found"}}, status: :not_found
  end

end
