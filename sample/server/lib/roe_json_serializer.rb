require 'active_model_serializers'

class RoeJSONSerializer < ActiveModel::Serializer::Adapter::Json

  def status_hash
    {
      show:     :FOUND,
      index:    :FOUND,
      create:   :CREATED,
      update:   :UPDATED,
      destroy:  :DELETED
    }
  end

  def serializable_hash(options = nil)

    method = options[:template].to_sym
    
    if serializer.object.respond_to?(:each) and serializer.object.empty?
      status = :NONE_FOUND
    elsif method == :update and serializer.object.previous_changes.empty?
        status = :NO_CHANGE
    else
      status = status_hash[method] || :SUCCESS
    end

    result = {
      status: status,
      data: ActiveModel::Serializer::Adapter::Attributes.new(serializer, instance_options).serializable_hash(options)
    }

    result[:pagination] = options[:pagination] unless options[:pagination].nil?

    return result
  end
end