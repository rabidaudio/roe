class AuthorsController < ApplicationController

  def index
    @authors = Author.where(params.permit(:email))
    @authors = @authors.order('created_at desc').paginate(pagination)
    render json: @authors, include: params[:include] || [], pagination: pagination_info
  end

  def show
    render json: Author.find(params[:id]), include: params[:include] || []
  end

  def create
    @author = Author.create! params.permit(:name, :email, :avatar)
    render json: @author, include: []
  end

  def update
    @author = Author.find params[:id]
    @author.update! params.permit(:name, :email, :avatar)
    render json: @author, include: []
  end

  def destroy
    @author = Author.find(params[:id])
    @author.destroy
    render json: @author, include: []
  end
end
