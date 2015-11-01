class AuthorsController < ApplicationController

  def index
    @authors = Author.where(params.permit(:email))
    render json: @authors
  end

  def show
    render json: Author.find(params[:id])
  end

  def create
    @author = Author.create! params.require(:author).permit(:name, :email, :avatar)
    render json: @author
  end

  def update
    @author = Author.find params[:id]
    if @author.nil?
      record_not_found
    else
      @author.update! params.require(:author).permit(:name, :email, :avatar)
      render json: @author
    end
  end

  def destroy
    render json: Author.find(params[:id]).destroy
  end

end
