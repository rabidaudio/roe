class ApplicationController < ActionController::API
  # include ActionController::Serialization
  # include ActionController::ImplicitRender
  
  class NotAuthenticated < StandardError
  end

  respond_to :json

  before_filter :require_ssl

  rescue_from ActiveRecord::RecordNotFound,             with: :not_found
  rescue_from CanCan::AccessDenied,                     with: :access_denied
  rescue_from NotAuthenticated,                         with: :not_authenticated
  rescue_from ActionController::UnknownController,      with: :no_route
  rescue_from ActionController::RoutingError,           with: :no_route
  # rescue_from ActionDispatch::ParamsParser::ParseError  with: :bad_format

  # fallback: none means if they aren't logged in, continue
  # acts_as_token_authentication_handler_for User, fallback: :none
  # if they aren't logged in, they will get caught here
  # before_action :check_authenticated

  # CanCan check permissions
  # load_and_authorize_resource except: [:status, :route_error, :create]

  # simple status check endpoint. Can be used to test if logged in
  def status
    render json: { data: { version: 2 } , status: "OK" }
  end

  # catchall route for unroutable methods
  def route_error
    raise ActionController::RoutingError.new params[:path]
  end


  private

  def not_found(exception)
    render_error :NOT_FOUND, exception.message, 404
  end

  def access_denied(exception)
    render_error :FORBIDDEN, exception.message, 403
  end

  def not_authenticated(exception)
    render_error :NOT_AUTHENTICATED, exception.message, 401
  end

  def no_route(exception)
    render_error :NO_SUCH_ROUTE, exception.message, 400
  end

  def bad_format(exception)
    render_error :BAD_FORMAT, exception.message, 400
  end

  # call this method if you want to disallow an operation
  def method_not_allowed
    render_error :METHOD_NOT_ALLOWED, "#{action_name} is not enabled for #{params[:controller]}", 405
  end

  def render_error(message, type, status)
    render json: { error: { type: type, message: message }, status: :ERROR }, status: status
  end
  ###

  def pagination
    {
      page: params[:page] || 1,
      per_page: [params[:per_page] || max_per_page, max_per_page].min
    }
  end

  def pagination_info
    p = pagination
    p[:page] = p[:page].to_i + 1
    pagination.merge(next_page: url_for(params: request.query_parameters.merge(p)))
  end

  def max_per_page
    50
  end

  def check_authenticated
    # TODO directions on how to obtain
    raise NotAuthenticated.new("Not logged in. Please supply `X-User-Email` and `X-User-Token` headers.") unless current_user
  end

  def require_ssl
    redirect_to protocol: "https://" unless request.ssl? || Rails.env.development? || request.local?
  end
end
