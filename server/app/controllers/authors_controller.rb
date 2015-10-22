class AuthorsController < ApplicationController

  def index
    @authors = Author.where(params.permit(:email))
    render json: @authors
  end

  def show
    render json: Author.find(params[:id])
  end

  def create
    @author = Author.create params.permit(:name, :email, :avatar)
    render json: @author
  end

  def update
    @author = Author.find params[:id]
    @author.update(params.permit(:name, :email, :avatar)) unless @author.nil?
    render json: @author
  end

  def destroy
    render json: Author.find(params[:id]).destroy
  end

end
