class ApplicationController < ActionController::API
  include ActionController::Serialization
  
  rescue_from ActiveRecord::RecordNotFound, with: :record_not_found
  # rescue_from User::NotAuthorized, with: :user_not_authorized
  # rescue_from User::Forbidden, with: :user_forbidden
 
  private
 
    def record_not_found
      render json: Hash.new, status: :not_found
    end

    # def user_not_authorized
    #   render json: Hash.new, status: 401
    # end

    # def user_forbidden
    #   render json: Hash.new, status: 403
    # end
end
